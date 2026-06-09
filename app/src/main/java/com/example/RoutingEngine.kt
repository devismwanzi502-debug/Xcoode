package com.example

import kotlin.math.max
import kotlin.math.min

/**
 * RoutingEngine handles network analysis metrics and evaluates potential routing paths.
 *
 * Game state synchronization protocols can easily mask static, low-variance latency (e.g., a steady 60ms ping)
 * through dead reckoning and client-side prediction. However, severe packet loss (>1%) and high jitter (>5ms)
 * result in sudden interpolation failures, teleportation (rubber-banding), and socket drops.
 *
 * This engine quantifies these network characteristics to select the absolute best transit node.
 */
object RoutingEngine {

    /**
     * Calculates the "Routing Quality Score" (0.0 to 100.0) from a 3-second telemetry window.
     *
     * Mathematical Weighting Logic:
     * - Latency Penalty: Each millisecond of round trip time is penalized linearly by 0.25. (60ms -> 15.0 penalty).
     * - Jitter Penalty: Jitter directly ruins position prediction algorithms. Penalized linear-exponentially at 3.0x. (2.0ms -> 6.0 penalty).
     * - Packet Loss Penalty: Packet loss is highly destructive, leading to packet re-transmission stalls or visual snaps.
     *   Penalized extremely harshly at 15.0x. Any loss >= 6.67% drops score below playable bounds entirely.
     *
     * Why 60ms (0% loss, 1.4ms jitter) beats 40ms (4.0% loss, 15ms jitter):
     * - Node A (Stable 60ms):
     *     Penalty = (60 * 0.25) + (1.4 * 3.0) + (0 * 15.0) = 15.0 + 4.2 + 0.0 = 19.2
     *     Score = 100.0 - 19.2 = 80.8 (Excellent/Optimized)
     * - Node B (Spiking 40ms):
     *     Penalty = (40 * 0.25) + (15 * 3.0) + (4 * 15.0) = 10.0 + 45.0 + 60.0 = 115.0
     *     Score = 100.0 - 115.0 = -15.0 -> Capped at 0.0 (Unplayable)
     */
    fun calculateQualityScore(
        latencyMs: Double,
        jitterMs: Double,
        packetLossPercent: Double
    ): Double {
        val latencyPenalty = latencyMs * 0.25
        val jitterPenalty = jitterMs * 3.0
        val packetLossPenalty = packetLossPercent * 15.0

        val score = 100.0 - latencyPenalty - jitterPenalty - packetLossPenalty
        return max(0.0, min(100.0, score))
    }

    /**
     * Structural representations of nodes
     */
    data class RoutingNode(
        val id: String,
        val label: String,
        val ipAddress: String,
        val region: String,
        val baseLatencyMs: Double,
        val baseJitterMs: Double,
        val basePacketLossPercent: Double
    ) {
        val qualityScore: Double
            get() = calculateQualityScore(baseLatencyMs, baseJitterMs, basePacketLossPercent)
    }

    // Default set of regional gaming booster proxy nodes
    val defaultNodes = listOf(
        RoutingNode(
            id = "aws-useast-1-edge",
            label = "AWS US-East Edge (Virginia)",
            ipAddress = "172.31.25.104",
            region = "North America",
            baseLatencyMs = 32.0,
            baseJitterMs = 1.4,
            basePacketLossPercent = 0.0
        ),
        RoutingNode(
            id = "gcp-euwest-3-edge",
            label = "GCP Europe-West Edge (Frankfurt)",
            ipAddress = "35.198.112.56",
            region = "Europe",
            baseLatencyMs = 18.0,
            baseJitterMs = 0.8,
            basePacketLossPercent = 0.0
        ),
        RoutingNode(
            id = "aws-apsoutheast-1-edge",
            label = "AWS Asia-Pacific Edge (Singapore)",
            ipAddress = "54.254.91.205",
            region = "Asia Pacific",
            baseLatencyMs = 68.0,
            baseJitterMs = 2.1,
            basePacketLossPercent = 0.05
        ),
        RoutingNode(
            id = "congested-public-node",
            label = "Public Dynamic Transit (Unoptimized ISP)",
            ipAddress = "203.0.113.44",
            region = "Global Public Route",
            baseLatencyMs = 45.0,
            baseJitterMs = 11.5,
            basePacketLossPercent = 3.5
        ),
        RoutingNode(
            id = "brazil-edge-node",
            label = "GCP South-America Edge (São Paulo)",
            ipAddress = "35.226.4.19",
            region = "South America",
            baseLatencyMs = 124.0,
            baseJitterMs = 3.0,
            basePacketLossPercent = 0.1
        )
    )
}
