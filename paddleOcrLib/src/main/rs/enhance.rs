#pragma version(1)
#pragma rs java_package_name(com.example.paddleocrlib)
#pragma rs_fp_relaxed

rs_allocation input;
rs_allocation blur;
rs_allocation histogram;

uint32_t width;
uint32_t height;

uchar4 __attribute__((kernel)) root(uint32_t x, uint32_t y) {
    // Get input and blurred pixels
    uchar4 inputPixel = rsGetElementAt_uchar4(input, x, y);
    uchar4 blurPixel = rsGetElementAt_uchar4(blur, x, y);

    // Compute the difference between input and blurred pixels
    float4 diff = convert_float4(inputPixel) - convert_float4(blurPixel);

    // Get histogram value
    uint32_t histogramIndex = (uint32_t)(clamp(rsUnpackColor8888(inputPixel).r, 0.0f, 1.0f) * 255.0f);
    uint32_t histogramValue = rsGetElementAt_uint(histogram, histogramIndex);

    // Apply local contrast enhancement
    float4 outputPixel = convert_float4(inputPixel) + diff * (float) histogramValue / (float)(width * height);
    outputPixel = clamp(outputPixel, 0.0f, 255.0f);

    return convert_uchar4(outputPixel);
}