#pragma version(1)
#pragma rs java_package_name(com.example.paddleocrlib)
#pragma rs_fp_relaxed

rs_allocation inputImage;

uchar4 __attribute__((kernel)) blur_detector(uint32_t x, uint32_t y) {
    float3 LoG[9] = { { 0, 1, 0 },
                      { 1, -4, 1 },
                      { 0, 1, 0 } };

    float3 sum = { 0, 0, 0 };
    for (int32_t j = -1; j <= 1; j++) {
        for (int32_t i = -1; i <= 1; i++) {
            float3 currentPixel = rsUnpackColor8888(rsGetElementAt_uchar4(inputImage, x + i, y + j)).rgb;
            sum += currentPixel * LoG[(j + 1) * 3 + (i + 1)];
        }
    }

    float3 output = {clamp(sum.r, 0.0f, 1.0f), clamp(sum.g, 0.0f, 1.0f), clamp(sum.b, 0.0f, 1.0f)};
    return rsPackColorTo8888(output);
}
