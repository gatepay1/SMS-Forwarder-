plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "world.gatepay.sms"
    compileSdk = 34
    defaultConfig {
        applicationId = "world.gatepay.sms"
        minSdk = 24; targetSdk = 34
        versionCode = 3; versionName = "3.0.0"
        // সার্ভারের ঠিকানা — দরকার হলে এখানে বদলান
        buildConfigField("String", "API_URL", "\"https://zazpdfjefmcfdqhuhhkj.supabase.co/functions/v1/sms-listener\"")
    }
    buildFeatures { buildConfig = true }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.13.1") }
