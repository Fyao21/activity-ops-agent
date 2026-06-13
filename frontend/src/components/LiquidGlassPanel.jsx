export default function LiquidGlassPanel({
  children,
  className = "",
  shellClassName = "",
  cornerRadius = 32,
  style,
  // These props are accepted but unused (were for liquid-glass-react)
  displacementScale,
  blurAmount,
  saturation,
  aberrationIntensity,
  elasticity,
  mode,
  mouseContainer,
  overLight,
  padding,
  ...props
}) {
  return (
    <div
      className={`liquid-glass-shell ${shellClassName}`}
      style={{ "--liquid-radius": `${cornerRadius}px` }}
    >
      <div
        className={`liquid-glass-panel ${className}`}
        style={{ borderRadius: cornerRadius, ...style }}
        {...props}
      >
        {children}
      </div>
    </div>
  );
}
