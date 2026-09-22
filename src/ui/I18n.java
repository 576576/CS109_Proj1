package ui;

import util.Log;
import util.ResourceRoot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * 界面文案。启动时按系统 Locale 挑一份 resource/i18n/messages_<tag>.properties，
 * 挑不到就回退英文。加一门语言往 SUPPORTED 里加一个 Locale 再放一份同名文件即可。
 */
public final class I18n {

    private static final String DIRECTORY = "i18n";
    private static final String PREFIX = "messages_";
    private static final String SUFFIX = ".properties";
    /** 没有命中的语言一律回退英文，而不是 JVM 的默认 Locale。 */
    public static final Locale FALLBACK = Locale.ENGLISH;
    /** 这几个地区按繁体处理，其余中文地区（含简体 zh_CN）走 zh_CN。 */
    private static final Set<String> TRADITIONAL = Set.of("TW", "HK", "MO");

    public static final List<Locale> SUPPORTED = List.of(
            Locale.ENGLISH,
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            Locale.JAPANESE,
            Locale.KOREAN,
            Locale.FRENCH,
            Locale.forLanguageTag("ru"));

    private static Locale locale = detect();
    private static boolean auto = true;
    private static ResourceBundle bundle = load(locale);
    private static ResourceBundle fallback;

    private I18n() {
    }

    /** 当前语言。 */
    public static Locale locale() {
        return locale;
    }

    /** 是否跟随系统语言。 */
    public static boolean isAuto() {
        return auto;
    }

    public static List<Locale> supported() {
        return SUPPORTED;
    }

    /** 切换语言；传 null 表示回到"跟随系统"。调用方负责把界面重建一遍。 */
    public static void setLocale(Locale wanted) {
        auto = wanted == null;
        locale = auto ? detect() : wanted;
        bundle = load(locale);
    }

    /** 取一条文案；当前语言里没有就回退英文，键真的不存在才抛异常。 */
    public static String tr(String key) {
        String value = lookup(key);
        if (value != null) return value;
        Log.warn("Missing i18n key: " + key);
        return key;
    }

    /** 语言用它自己的名字显示；中文按简繁两种字形分别标成「中文（简体）/中文（繁体）」。 */
    public static String name(Locale option) {
        if (isChinese(option)) {
            String label = lookup(traditional(option) ? "language.zh_TW" : "language.zh_CN");
            return label != null ? label : (traditional(option) ? "中文（繁体）" : "中文（简体）");
        }
        String display = option.getDisplayName(option);
        return display.isBlank() ? option.toLanguageTag() : display;
    }

    /** 先查当前语言，再查英文；都没有才返回 null。 */
    private static String lookup(String key) {
        if (bundle.containsKey(key)) return bundle.getString(key);
        if (fallback == null) fallback = load(FALLBACK);
        return fallback.containsKey(key) ? fallback.getString(key) : null;
    }

    /** 下拉框里的一项，toString 直接是显示名。 */
    public record Language(Locale locale, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** 列表第一项是"自动"，它不带 Locale，选它就回到跟随系统。 */
    public static List<Language> languages() {
        List<Language> options = new ArrayList<>();
        options.add(new Language(null, lookup("language.auto") == null ? "Auto (system)" : lookup("language.auto")));
        SUPPORTED.forEach(option -> options.add(new Language(option, name(option))));
        return List.copyOf(options);
    }

    /** 系统语言命中支持列表就用它，否则英文。可用 -Dmatch3.lang=ja 强制指定。 */
    private static Locale detect() {
        Locale override = override();
        Locale system = override != null ? override : Locale.getDefault();
        for (Locale supported : SUPPORTED) {
            if (!supported.getLanguage().equals(system.getLanguage())) continue;
            if (isChinese(system)) continue;
            return supported;
        }
        if (isChinese(system)) return traditional(system) ? Locale.TRADITIONAL_CHINESE : Locale.SIMPLIFIED_CHINESE;
        return FALLBACK;
    }

    private static Locale override() {
        String tag = System.getProperty("match3.lang");
        if (tag == null || tag.isBlank()) return null;
        // 容忍 zh_CN 这种下划线写法，forLanguageTag 只认连字符
        return Locale.forLanguageTag(tag.trim().replace('_', '-'));
    }

    private static boolean isChinese(Locale locale) {
        return "zh".equals(locale.getLanguage());
    }

    private static boolean traditional(Locale locale) {
        return TRADITIONAL.contains(locale.getCountry());
    }

    /** 依次试 <语言>_<地区> 和 语言 两级，例如 zh_TW → zh_TW、zh。 */
    private static List<String> tags(Locale locale) {
        String language = locale.getLanguage();
        if (language.isEmpty()) return List.of();
        List<String> tags = new ArrayList<>();
        String country = locale.getCountry();
        if (!country.isEmpty()) tags.add(language + "_" + country);
        if (isChinese(locale)) tags.add(traditional(locale) ? "zh_TW" : "zh_CN");
        else tags.add(language);
        return tags;
    }

    private static ResourceBundle load(Locale locale) {
        for (String tag : tags(locale)) {
            ResourceBundle loaded = tryLoad(tag);
            if (loaded != null) return loaded;
        }
        Log.warn("No i18n bundle for " + locale + ", falling back to English");
        ResourceBundle english = tryLoad("en");
        if (english == null) throw new IllegalStateException("Missing " + fileOf("en"));
        return english;
    }

    private static ResourceBundle tryLoad(String tag) {
        Path file = fileOf(tag);
        if (!Files.isRegularFile(file)) return null;
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
        } catch (IOException | RuntimeException e) {
            Log.warn("Cannot read " + file + ": " + e);
            return null;
        }
    }

    private static Path fileOf(String tag) {
        return ResourceRoot.path(DIRECTORY + "/" + PREFIX + tag + SUFFIX);
    }
}
