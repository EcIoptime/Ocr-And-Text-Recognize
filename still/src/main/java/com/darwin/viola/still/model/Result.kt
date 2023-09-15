package com.darwin.viola.still.model

import com.google.mlkit.vision.face.Face

/**
 * The class Result
 *
 * @author Darwin Francis
 * @version 1.0
 * @since 09 Jul 2020
 */
data class Result(val faceCount: Int, val facePortraits: List<FacePortrait>)
