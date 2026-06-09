import { useCallback, useRef, useState } from "react";

const CHAT_KEY = "activity_agent_chats";

function loadChats() {
  try {
    const raw = localStorage.getItem(CHAT_KEY);
    const chats = raw ? JSON.parse(raw) : [];
    return Array.isArray(chats) ? chats : [];
  } catch {
    return [];
  }
}

function saveChats(chats) {
  localStorage.setItem(CHAT_KEY, JSON.stringify(chats));
}

function makeTitle(text) {
  const t = (text || "").replace(/\s+/g, " ").trim();
  if (!t) return "新对话";
  return t.length <= 15 ? t : t.slice(0, 15) + "...";
}

export function useConversations() {
  const chatsRef = useRef(loadChats());
  const [conversations, setConversations] = useState(chatsRef.current);
  const [activeId, setActiveId] = useState(null);

  const persist = useCallback((next) => {
    chatsRef.current = next;
    setConversations(next);
    saveChats(next);
    return next;
  }, []);

  const startNew = useCallback(() => setActiveId(null), []);

  const select = useCallback((id) => setActiveId(id), []);

  const addMessage = useCallback(
    (msg) => {
      if (!activeId) {
        const conv = {
          id: crypto.randomUUID(),
          title: makeTitle(msg.text),
          createdAt: Date.now(),
          messages: [msg],
        };
        persist([conv, ...chatsRef.current]);
        setActiveId(conv.id);
        return conv;
      }
      let target = null;
      const next = chatsRef.current.map((c) => {
        if (c.id !== activeId) return c;
        target = { ...c, messages: [...c.messages, msg] };
        return target;
      });
      persist(next);
      return target;
    },
    [activeId, persist]
  );

  const replaceConversation = useCallback(
    (id, updater) => {
      let updated = null;
      const next = chatsRef.current.map((c) => {
        if (c.id !== id) return c;
        updated = updater(c);
        return updated;
      });
      persist(next);
      return updated;
    },
    [persist]
  );

  const activeConversation = conversations.find((c) => c.id === activeId) ?? null;

  return {
    activeConversation,
    activeId,
    addMessage,
    conversations,
    replaceConversation,
    selectConversation: select,
    startNewConversation: startNew,
  };
}
