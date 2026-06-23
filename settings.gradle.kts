pluginManagement {
    repositories {
        // 阿里云镜像（优先）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 华为镜像（备用）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        // 官方仓库（最后兜底）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "CookMate2"
include(":app")