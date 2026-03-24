package com.aiplatform.bidding.dto.response;

import java.util.List;

public record VisualizationDto(
    PieChartData coverageChart,
    BarChartData issueDistribution,
    GaugeData riskGauge
) {};

record PieChartData(
    List<String> labels,
    List<Integer> data
) {}

record BarChartData(
    List<String> labels,
    List<Integer> data
) {}

record GaugeData(
    double value,
    double min,
    double max,
    List<GaugeThreshold> thresholds
) {}

record GaugeThreshold(
    double from,
    double to,
    String color
) {}
