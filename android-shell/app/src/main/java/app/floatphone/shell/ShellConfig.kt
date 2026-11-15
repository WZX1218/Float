package app.floatphone.shell

import android.content.Context
import android.net.Uri

/**
 * 站点地址的唯一来源。
 *
 * 改动背景：原来站点地址只来自 BuildConfig.SITE_URL（编译期烧进 APK），
 * 域名一换、Vercel 项目一迁，全网旧包集体报废，必须让每个用户重装。
 * 现在 BuildConfig 只作首次兜底，用户在壳里改一次就落到 SharedPreferences；
 * PushService / 来电接收器这些没有 Activity 的地方也统一从这里取，避免两处地址不一致。
 */
object ShellConfig {
    private const val PREFS = "float_shell"
    private const val KEY_SITE = "site_url"

    /**
     * 编译期兜底是否可用：Actions 里 SHELL_SITE_URL 没配时会被传成空字符串，
     * 占位符 example.invalid 也一样，都算"不可用"，需要用户自己填。
     */
    private val hasUsableBuildDefault: Boolean
        get() = BuildConfig.SITE_URL.let { it.startsWith("http") && !it.contains("example.invalid") }

    /** 当前生效的站点地址（去掉结尾斜杠）。 */
    fun get(ctx: Context): String {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SITE, null)
        val url = saved?.takeIf { it.isNotBlank() } ?: BuildConfig.SITE_URL
        return url.trim().trimEnd('/')
    }

    /** 用户手动设置站点地址。 */
    fun set(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SITE, url.trim().trimEnd('/'))
            .apply()
    }

    /** 首次启动是否需要向用户要地址：编译期兜底不可用，且没人设置过。 */
    fun needsSetup(ctx: Context): Boolean {
        val saved = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SITE, null)
        return saved.isNullOrBlank() && !hasUsableBuildDefault
    }

    /** 站点 host，用于判断站内导航还是外链。 */
    fun host(ctx: Context): String =
        runCatching { Uri.parse(get(ctx)).host.orEmpty() }.getOrDefault("")
}
