import LiquidGlass from "liquid-glass-react";

export default function LiquidGlassPanel({
  children,
  className = "",
  shellClassName = "",
  cornerRadius = 32,
  displacementScale = 70,
  blurAmount = 0.0625,
  saturation = 140,
  aberrationIntensity = 2,
  elasticity = 0.15,
  mode = "prominent",
  mouseContainer,
  overLight = false,
  padding = "0",
  style,
  ...props
}) {
  return (
    <div
      className={`liquid-glass-shell ${shellClassName}`}
      style={{ "--liquid-radius": `${cornerRadius}px` }}
    >
      <LiquidGlass
        className={`liquid-glass-panel ${className}`}
        cornerRadius={cornerRadius}
        displacementScale={displacementScale}
        blurAmount={blurAmount}
        saturation={saturation}
        aberrationIntensity={aberrationIntensity}
        elasticity={elasticity}
        mode={mode}
        mouseContainer={mouseContainer}
        overLight={overLight}
        padding={padding}
        style={{
          position: "relative",
          top: "auto",
          left: "auto",
          ...style,
        }}
        {...props}
      >
        {children}
      </LiquidGlass>
    </div>
  );
}
