package com.apolloauto.player.media

import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFeaturesBandsTest {
    @Test
    fun bassHeavyFft_raisesBassOverTreble() {
        val fft = FloatArray(48) { i -> if (i < 6) 0.95f else 0.05f }
        val bands = SpectralBandExtractor.extract(fft)
        assertTrue(bands.bass > bands.treble)
        assertTrue(bands.subBass > bands.presence)
    }

    @Test
    fun trebleHeavyFft_raisesTrebleOverBass() {
        val fft = FloatArray(48) { i -> if (i > 36) 0.95f else 0.05f }
        val bands = SpectralBandExtractor.extract(fft)
        assertTrue(bands.treble > bands.bass)
        assertTrue(bands.presence > bands.subBass)
    }

    @Test
    fun midHeavyFft_raisesMids() {
        val fft = FloatArray(48) { i -> if (i in 12..24) 0.9f else 0.05f }
        val bands = SpectralBandExtractor.extract(fft)
        assertTrue(bands.lowMid > bands.bass)
        assertTrue(bands.upperMid > bands.treble * 0.5f)
    }

    @Test
    fun flux_detectsRisingEnergy() {
        val prev = FloatArray(16) { 0.1f }
        val next = FloatArray(16) { 0.8f }
        val f = SpectralBandExtractor.flux(prev, next)
        assertTrue(f > 0.3f)
    }

    @Test
    fun profile_psytranceBoostsBassAndTransients() {
        val base = AudioFeatures(bass = 0.5f, onset = 0.4f, spectralFlux = 0.4f)
        val psy = base.withProfile(TranceResponseProfile.PSYTRANCE)
        assertTrue(psy.bass > base.bass)
        assertTrue(psy.onset > base.onset)
    }
}
