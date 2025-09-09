package com.pcloudai.backend

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import com.google.inject.name.Named
import com.google.inject.name.Names
import com.pcloudai.backend.api.AuthResource
import com.pcloudai.backend.api.FileResource
import com.pcloudai.backend.api.HealthResource
import com.pcloudai.backend.api.MetricsResource
import com.pcloudai.backend.api.UserResource
import com.pcloudai.backend.auth.JwtAuthFilter
import com.pcloudai.backend.auth.JwtAuthenticator
import com.pcloudai.backend.auth.JwtHelper
import com.pcloudai.backend.auth.UserPrincipal
import com.pcloudai.backend.auth.UserService
import com.pcloudai.backend.core.repository.HibernateFileRepository
import com.pcloudai.backend.core.repository.HibernateUserRepository
import com.pcloudai.backend.core.repository.HibernateUserSettingsRepository
import com.pcloudai.backend.core.service.FileService
import com.pcloudai.backend.core.service.UserProfileService
import com.pcloudai.backend.core.service.UserSettingsService
import com.pcloudai.backend.preview.generator.AudioPreviewGenerator
import com.pcloudai.backend.preview.generator.ImagePreviewGenerator
import com.pcloudai.backend.preview.generator.PdfPreviewGenerator
import com.pcloudai.backend.preview.generator.TextPreviewGenerator
import com.pcloudai.backend.preview.generator.VideoPreviewGenerator
import com.pcloudai.backend.preview.model.PreviewMetrics
import com.pcloudai.backend.preview.queue.PreviewQueue
import com.pcloudai.backend.preview.worker.PreviewWorker
import com.pcloudai.backend.util.PasswordUtils
import io.dropwizard.auth.AuthDynamicFeature
import io.dropwizard.auth.Authenticator
import io.dropwizard.hibernate.HibernateBundle
import io.dropwizard.setup.Environment
import org.hibernate.SessionFactory

class PCloudGuiceModule(
    private val config: PCloudConfiguration,
    private val environment: Environment,
    private val hibernateBundle: HibernateBundle<*>
) : AbstractModule() {

    override fun configure() {
        // --- 1. Repository Bindings ---
        bind(HibernateUserRepository::class.java).`in`(Singleton::class.java)
        bind(HibernateFileRepository::class.java).`in`(Singleton::class.java)
        bind(HibernateUserSettingsRepository::class.java).`in`(Singleton::class.java)

        // --- 2. Service Bindings ---
        bind(UserService::class.java).`in`(Singleton::class.java)
        bind(FileService::class.java).`in`(Singleton::class.java)
        bind(UserProfileService::class.java).`in`(Singleton::class.java)
        bind(UserSettingsService::class.java).`in`(Singleton::class.java)

        // --- 3. Preview Generator Bindings ---
        bind(ImagePreviewGenerator::class.java).`in`(Singleton::class.java)
        bind(PdfPreviewGenerator::class.java).`in`(Singleton::class.java)
        bind(TextPreviewGenerator::class.java).`in`(Singleton::class.java)
        bind(AudioPreviewGenerator::class.java).`in`(Singleton::class.java)
        bind(VideoPreviewGenerator::class.java).`in`(Singleton::class.java)

        // --- 4. Authentication Bindings ---
        // Bind the Authenticator interface to our concrete implementation.
        bind(object : TypeLiteral<Authenticator<String, UserPrincipal>>() {})
            .to(JwtAuthenticator::class.java).`in`(Singleton::class.java)

        // Bind the realm string using a named annotation.
        bind(String::class.java).annotatedWith(Names.named("jwtRealm"))
            .toInstance(config.jwt.realm)

        // ADD THIS LINE: Tell Guice how to provide the JwtAuthFilter as a singleton.
        // Guice will see its @Inject constructor and automatically provide its dependencies.
        bind(JwtAuthFilter::class.java).`in`(Singleton::class.java)

        // --- 5. Resource Bindings ---
        // All resources are singletons. Guice will handle their dependencies.
        bind(AuthResource::class.java).`in`(Singleton::class.java)
        bind(FileResource::class.java).`in`(Singleton::class.java)
        bind(UserResource::class.java).`in`(Singleton::class.java)
        bind(HealthResource::class.java).`in`(Singleton::class.java)
        bind(MetricsResource::class.java).`in`(Singleton::class.java)
    }

    // --- @Provides Methods ---
    // These methods provide instances that cannot be created with a simple constructor.

    @Provides
    fun providesSessionFactory(): SessionFactory = hibernateBundle.sessionFactory

    @Provides
    @Singleton
    fun providesPasswordUtils(): PasswordUtils {
        return PasswordUtils(
            logRounds = config.security.passwordLogRounds,
            pepper = config.security.passwordPepper
        )
    }

    @Provides
    @Singleton
    fun providesJwtHelper(): JwtHelper {
        return JwtHelper(config.jwt.secret, config.jwt.expirationMinutes)
    }

    @Provides
    @Singleton
    fun providesPreviewQueue(): PreviewQueue {
        return PreviewQueue(config, environment.objectMapper)
    }

    @Provides
    @Singleton
    fun providesPreviewMetrics(): PreviewMetrics {
        return PreviewMetrics(environment.metrics())
    }

    @Provides
    @Singleton
    fun providesPreviewWorkers(
        imageGen: ImagePreviewGenerator,
        pdfGen: PdfPreviewGenerator,
        textGen: TextPreviewGenerator,
        audioGen: AudioPreviewGenerator,
        videoGen: VideoPreviewGenerator,
        metrics: PreviewMetrics
    ): List<PreviewWorker> {
        val workers = (0 until config.preview.workerCount).map {
            PreviewWorker(
                config,
                imageGen,
                pdfGen,
                textGen,
                audioGen,
                videoGen,
                metrics
            )
        }
        // Start workers after they are created and bound
        workers.forEach { it.start() }
        return workers
    }
}
