package de.artemis.omniwheel.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Immutable collections and UTF-8 storage shared by the Java 8 port.
 */
public final class Java8 {

    private Java8() {
    }

    @SafeVarargs
    public static <T> List<T> list(T... values) {
        return copyList(Arrays.asList(values));
    }

    public static <T> List<T> copyList(Collection<? extends T> values) {
        List<T> copy = new ArrayList<T>(values);
        for (T value : copy) Objects.requireNonNull(value);
        return Collections.unmodifiableList(copy);
    }

    @SafeVarargs
    public static <T> Set<T> set(T... values) {
        return copySet(Arrays.asList(values));
    }

    public static <T> Set<T> copySet(Collection<? extends T> values) {
        Set<T> copy = new LinkedHashSet<T>(values);
        for (T value : copy) Objects.requireNonNull(value);
        return Collections.unmodifiableSet(copy);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... pairs) {
        Map<K, V> result = new LinkedHashMap<K, V>();
        for (int i = 0; i < pairs.length; i += 2) result.put((K) Objects.requireNonNull(pairs[i]), (V) Objects.requireNonNull(pairs[i + 1]));
        return Collections.unmodifiableMap(result);
    }

    public static <K, V> Map<K, V> copyMap(Map<? extends K, ? extends V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<K, V>(source));
    }

    public static <T> Collector<T, ?, List<T>> toList() {
        return Collectors.collectingAndThen(Collectors.<T>toList(), Java8::copyList);
    }

    public static String readString(Path path, java.nio.charset.Charset charset) throws IOException {
        return new String(Files.readAllBytes(path), charset);
    }

    public static byte[] readAllBytes(java.io.InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int count; (count = input.read(buffer)) != -1; ) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    public static Path writeString(Path path, CharSequence text, java.nio.charset.Charset charset, OpenOption... options) throws IOException {
        return Files.write(path, text.toString().getBytes(charset), options);
    }

    public static String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public static Path writeString(Path path, CharSequence text, OpenOption... options) throws IOException {
        return Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8), options);
    }

    public static <T> T value(Supplier<T> body) {
        return body.get();
    }

    public static <T> T requireNonNullElse(T value, T fallback) {
        if (value != null) {
            return value;
        } else {
            return Objects.requireNonNull(fallback);
        }
    }

    public static boolean isBlank(String text) {
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (!Character.isWhitespace(codePoint))
                return false;
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    public static String strip(String text) {
        int start = 0, end = text.length();
        while (start < end && Character.isWhitespace(text.codePointAt(start))) start += Character.charCount(text.codePointAt(start));
        while (end > start && Character.isWhitespace(text.codePointBefore(end))) end -= Character.charCount(text.codePointBefore(end));
        return text.substring(start, end);
    }

    public static String repeat(String text, int count) {
        if (count < 0)
            throw new IllegalArgumentException("Negative repeat count");
        StringBuilder result = new StringBuilder(Math.multiplyExact(text.length(), count));
        for (int i = 0; i < count; i++) result.append(text);
        return result.toString();
    }

    public static <T, K, V> Collector<T, ?, Map<K, V>> toUnmodifiableMap(java.util.function.Function<? super T, ? extends K> key, java.util.function.Function<? super T, ? extends V> value) {
        return Collectors.collectingAndThen(Collectors.toMap(key, value), Java8::copyMap);
    }

    public static long mismatch(Path first, Path second) throws IOException {
        try (java.io.InputStream a = new java.io.BufferedInputStream(Files.newInputStream(first));
            java.io.InputStream b = new java.io.BufferedInputStream(Files.newInputStream(second))) {
            for (long offset = 0; ; offset++) {
                int left = a.read(), right = b.read();
                if (left != right)
                    return offset;
                if (left < 0)
                    return -1;
            }
        }
    }

    public static <T> T fail(RuntimeException failure) {
        throw failure;
    }

    public static final class HexFormat {

        private HexFormat() {
        }

        public static HexFormat of() {
            return new HexFormat();
        }

        public String formatHex(byte[] bytes) {
            char[] result = new char[bytes.length * 2];
            char[] digits = "0123456789abcdef".toCharArray();
            for (int i = 0; i < bytes.length; i++) {
                result[i * 2] = digits[(bytes[i] >>> 4) & 15];
                result[i * 2 + 1] = digits[bytes[i] & 15];
            }
            return new String(result);
        }
    }
}
