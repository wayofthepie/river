package cd.river

import cd.river.task.TaskHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router


@Configuration
class Routes(private val taskHandler: TaskHandler) {
    @Bean
    fun router() = router {
        accept(MediaType.APPLICATION_JSON).nest {
            POST("/", taskHandler::create)
        }
    }
}