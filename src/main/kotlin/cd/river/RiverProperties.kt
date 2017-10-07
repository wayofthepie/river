package cd.river

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("task")
data class RiverProperties(var queueLimit: Long = 1)