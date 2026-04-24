#version 150

#moj_import <dantes:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform vec4 ColorModulator;

// Gradient corner colors
uniform vec4 TopLeftColor;
uniform vec4 BottomLeftColor;
uniform vec4 TopRightColor;
uniform vec4 BottomRightColor;

out vec4 OutColor;

vec4 bilinearInterpolation(vec2 uv) {
    // Horizontal interpolation for the top edge
    vec4 topColor = mix(TopLeftColor, TopRightColor, uv.x);

    // Horizontal interpolation for the bottom edge
    vec4 bottomColor = mix(BottomLeftColor, BottomRightColor, uv.x);

    // Vertical interpolation between top and bottom colors
    return mix(topColor, bottomColor, uv.y);
}

void main() {
    vec2 center = Size * 0.5;
    vec2 uv = FragCoord; // UV coordinates from 0 to 1

    // Compute gradient color for current fragment
    vec4 gradientColor = bilinearInterpolation(uv);

    float distance = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    vec4 finalColor = vec4(gradientColor.rgb, gradientColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}
