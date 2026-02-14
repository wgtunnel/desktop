object SystemVar {
    fun fromEnvironment(envVar: String): String? {
        return System.getenv(envVar)
    }
}
