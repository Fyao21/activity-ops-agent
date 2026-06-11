package com.example.activityagent.mq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Manual observation test.
 * Run this test alone when you want to keep the Spring test container alive
 * and watch RocketMQ consumer logs in the IDEA Run console.
 *
 * Prerequisite:
 * 1. Start NameServer: mqnamesrv.cmd
 * 2. Start Broker: mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
 *
 * Suggested usage:
 * 1. Run this test first and keep it alive
 * 2. Then run RocketMqSendTest in another test run
 * 3. Watch the consumer log output in this test console
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class RocketMqManualObserveTest {

    @Test
    void keepApplicationAliveForManualObservation() throws Exception {
        log.info("RocketMQ manual observe test started. Keeping Spring test context alive for 60 seconds...");
        Thread.sleep(60000);
        log.info("RocketMQ manual observe test finished.");
    }
}
