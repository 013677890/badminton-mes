package com.badminton.mes.performance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * M1/M2 只读接口的无第三方依赖压力测试运行器。
 *
 * <p>支持条码解析、任务进度和我的工序三个场景，并以失败率、P95 响应时间作为门禁。
 * 运行器只访问调用者明确提供的环境和数据，不负责创建或修改生产业务数据。
 *
 * @author 范家权
 */
public final class M1M2ApiPressureRunner {

    private static final Pattern SUCCESS_CODE =
            Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"00000\\\"");

    private M1M2ApiPressureRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (Arrays.asList(args).contains("--help") || args.length == 0) {
            printUsage();
            return;
        }

        Config config = Config.parse(args);
        HttpRequest request = buildRequest(config);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds()))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        System.out.printf(Locale.ROOT,
                "[%s] warmup: scenario=%s, requests=%d, concurrency=%d%n",
                LocalDateTime.now(), config.scenario().cliName,
                config.warmupRequests(), config.concurrency());
        List<Sample> warmup = runRound(client, request,
                config.warmupRequests(), config.concurrency(), config.timeoutSeconds());
        long warmupFailures = warmup.stream().filter(sample -> !sample.success()).count();
        if (warmupFailures > 0) {
            Sample firstFailure = warmup.stream().filter(sample -> !sample.success()).findFirst().orElseThrow();
            System.err.printf(Locale.ROOT,
                    "Warmup failed: failures=%d/%d, firstStatus=%d, firstBody=%s%n",
                    warmupFailures, warmup.size(), firstFailure.statusCode(), firstFailure.responsePreview());
            System.exit(3);
        }

        System.out.printf(Locale.ROOT, "[%s] measurement started%n", LocalDateTime.now());
        long startedAt = System.nanoTime();
        List<Sample> samples = runRound(client, request,
                config.requests(), config.concurrency(), config.timeoutSeconds());
        long elapsedNanos = System.nanoTime() - startedAt;

        Result result = Result.from(samples, elapsedNanos);
        result.print(config);
        if (result.failureRate() > config.maxFailureRate()
                || result.p95Millis() > config.maxP95Millis()) {
            System.err.println("PRESSURE_TEST_FAILED: threshold exceeded");
            System.exit(2);
        }
        System.out.println("PRESSURE_TEST_PASSED");
    }

    private static List<Sample> runRound(HttpClient client, HttpRequest request,
                                         int requests, int concurrency,
                                         int timeoutSeconds) throws InterruptedException {
        List<Sample> samples = new ArrayList<>(requests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        try {
            for (int offset = 0; offset < requests; offset += concurrency) {
                int batchSize = Math.min(concurrency, requests - offset);
                List<Callable<Sample>> tasks = new ArrayList<>(batchSize);
                for (int index = 0; index < batchSize; index++) {
                    tasks.add(() -> execute(client, request, timeoutSeconds));
                }
                executor.invokeAll(tasks).forEach(future -> {
                    try {
                        samples.add(future.get());
                    } catch (Exception exception) {
                        samples.add(Sample.failed(exception));
                    }
                });
            }
            return samples;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Sample execute(HttpClient client, HttpRequest request, int timeoutSeconds) {
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            long duration = System.nanoTime() - startedAt;
            boolean success = response.statusCode() >= 200
                    && response.statusCode() < 300
                    && SUCCESS_CODE.matcher(response.body()).find();
            return new Sample(duration, response.statusCode(), success,
                    preview(response.body()));
        } catch (Exception exception) {
            long duration = System.nanoTime() - startedAt;
            return new Sample(duration, 0, false,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static HttpRequest buildRequest(Config config) {
        String url;
        String body = null;
        switch (config.scenario()) {
            case BARCODE_PARSE -> {
                url = config.baseUrl() + "/api/barcode/instances/parse";
                body = "{\"barcodeValue\":\"" + jsonEscape(config.barcodeValue()) + "\"}";
            }
            case TASK_PROGRESS ->
                    url = config.baseUrl() + "/api/scene/production_tasks/"
                            + config.taskId() + "/progress";
            case MY_OPERATIONS ->
                    url = config.baseUrl() + "/api/scene/operation_jobs/my";
            default -> throw new IllegalStateException("Unsupported scenario: " + config.scenario());
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(config.timeoutSeconds()))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + config.token());
        if (body == null) {
            return builder.GET().build();
        }
        return builder.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String preview(String body) {
        if (body == null) {
            return "";
        }
        String oneLine = body.replace('\r', ' ').replace('\n', ' ');
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200);
    }

    private static void printUsage() {
        System.out.println("""
                M1/M2 API pressure runner

                Required:
                  --scenario barcode-parse|task-progress|my-operations
                  --base-url http://127.0.0.1:8080
                  --token <bearer token>   (or environment MES_PRESSURE_TOKEN)

                Scenario data:
                  barcode-parse: --barcode <existing barcode value>
                  task-progress: --task-id <accessible task id>

                Optional:
                  --requests 1000
                  --concurrency 20
                  --warmup 20
                  --timeout-seconds 10
                  --max-failure-rate 0.01
                  --max-p95-ms 500
                """);
    }

    private enum Scenario {
        BARCODE_PARSE("barcode-parse"),
        TASK_PROGRESS("task-progress"),
        MY_OPERATIONS("my-operations");

        private final String cliName;

        Scenario(String cliName) {
            this.cliName = cliName;
        }

        private static Scenario parse(String value) {
            return Arrays.stream(values())
                    .filter(scenario -> scenario.cliName.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + value));
        }
    }

    private record Config(Scenario scenario, String baseUrl, String token,
                          String barcodeValue, Long taskId, int requests,
                          int concurrency, int warmupRequests, int timeoutSeconds,
                          double maxFailureRate, double maxP95Millis) {

        private static Config parse(String[] args) {
            Map<String, String> values = parseOptions(args);
            Scenario scenario = Scenario.parse(required(values, "scenario"));
            String baseUrl = required(values, "base-url").replaceAll("/+$", "");
            String token = values.getOrDefault("token", System.getenv("MES_PRESSURE_TOKEN"));
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing --token or MES_PRESSURE_TOKEN environment variable");
            }
            String barcode = values.get("barcode");
            Long taskId = values.containsKey("task-id")
                    ? Long.valueOf(values.get("task-id")) : null;
            if (scenario == Scenario.BARCODE_PARSE && (barcode == null || barcode.isBlank())) {
                throw new IllegalArgumentException("barcode-parse requires --barcode");
            }
            if (scenario == Scenario.TASK_PROGRESS && (taskId == null || taskId <= 0)) {
                throw new IllegalArgumentException("task-progress requires positive --task-id");
            }

            int requests = positiveInt(values, "requests", 1000);
            int concurrency = positiveInt(values, "concurrency", 20);
            int warmup = nonNegativeInt(values, "warmup", 20);
            int timeout = positiveInt(values, "timeout-seconds", 10);
            double maxFailureRate = ratio(values, "max-failure-rate", 0.01);
            double maxP95 = positiveDouble(values, "max-p95-ms", 500.0);
            return new Config(scenario, baseUrl, token, barcode, taskId,
                    requests, concurrency, warmup, timeout, maxFailureRate, maxP95);
        }

        private static Map<String, String> parseOptions(String[] args) {
            Map<String, String> values = new HashMap<>();
            for (int index = 0; index < args.length; index += 2) {
                if (!args[index].startsWith("--") || index + 1 >= args.length) {
                    throw new IllegalArgumentException("Options must use --name value pairs");
                }
                values.put(args[index].substring(2), args[index + 1]);
            }
            return values;
        }

        private static String required(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing --" + key);
            }
            return value;
        }

        private static int positiveInt(Map<String, String> values, String key, int defaultValue) {
            int value = Integer.parseInt(values.getOrDefault(key, String.valueOf(defaultValue)));
            if (value <= 0) {
                throw new IllegalArgumentException("--" + key + " must be positive");
            }
            return value;
        }

        private static int nonNegativeInt(Map<String, String> values, String key, int defaultValue) {
            int value = Integer.parseInt(values.getOrDefault(key, String.valueOf(defaultValue)));
            if (value < 0) {
                throw new IllegalArgumentException("--" + key + " must not be negative");
            }
            return value;
        }

        private static double ratio(Map<String, String> values, String key, double defaultValue) {
            double value = Double.parseDouble(values.getOrDefault(key, String.valueOf(defaultValue)));
            if (value < 0 || value > 1) {
                throw new IllegalArgumentException("--" + key + " must be between 0 and 1");
            }
            return value;
        }

        private static double positiveDouble(Map<String, String> values,
                                             String key, double defaultValue) {
            double value = Double.parseDouble(values.getOrDefault(key, String.valueOf(defaultValue)));
            if (value <= 0) {
                throw new IllegalArgumentException("--" + key + " must be positive");
            }
            return value;
        }
    }

    private record Sample(long durationNanos, int statusCode,
                          boolean success, String responsePreview) {

        private static Sample failed(Exception exception) {
            return new Sample(0, 0, false,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private record Result(int total, int succeeded, int failed, double failureRate,
                          double requestsPerSecond, double averageMillis,
                          double p50Millis, double p95Millis,
                          double p99Millis, double maxMillis, String firstFailure) {

        private static Result from(List<Sample> samples, long elapsedNanos) {
            double[] durations = samples.stream()
                    .mapToDouble(sample -> sample.durationNanos() / 1_000_000.0)
                    .sorted()
                    .toArray();
            int failed = (int) samples.stream().filter(sample -> !sample.success()).count();
            int succeeded = samples.size() - failed;
            double failureRate = samples.isEmpty() ? 0 : (double) failed / samples.size();
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            double rps = elapsedSeconds == 0 ? 0 : samples.size() / elapsedSeconds;
            double average = Arrays.stream(durations).average().orElse(0);
            String firstFailure = samples.stream()
                    .filter(sample -> !sample.success())
                    .map(sample -> "status=" + sample.statusCode() + ", body=" + sample.responsePreview())
                    .findFirst().orElse("none");
            return new Result(samples.size(), succeeded, failed, failureRate, rps,
                    average, percentile(durations, 0.50), percentile(durations, 0.95),
                    percentile(durations, 0.99),
                    durations.length == 0 ? 0 : durations[durations.length - 1], firstFailure);
        }

        private static double percentile(double[] sorted, double percentile) {
            if (sorted.length == 0) {
                return 0;
            }
            int index = (int) Math.ceil(percentile * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
        }

        private void print(Config config) {
            System.out.printf(Locale.ROOT, """
                    scenario=%s
                    total=%d, succeeded=%d, failed=%d, failureRate=%.4f
                    throughput=%.2f req/s
                    latencyMs: avg=%.2f, p50=%.2f, p95=%.2f, p99=%.2f, max=%.2f
                    thresholds: maxFailureRate=%.4f, maxP95Ms=%.2f
                    firstFailure=%s
                    """, config.scenario().cliName, total, succeeded, failed, failureRate,
                    requestsPerSecond, averageMillis, p50Millis, p95Millis, p99Millis,
                    maxMillis, config.maxFailureRate(), config.maxP95Millis(), firstFailure);
        }
    }
}
