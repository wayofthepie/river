package cd.river.config

import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnection
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class RedisConfig {
    @Bean
    fun reactiveRedisConnection(): ReactiveRedisConnection = lettuce().reactiveConnection


    @Bean
    fun lettuce(): LettuceConnectionFactory = LettuceConnectionFactory()


    @Bean
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilder
            = Jackson2ObjectMapperBuilder().modulesToInstall(KotlinModule())
}