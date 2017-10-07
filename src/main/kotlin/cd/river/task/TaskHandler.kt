package cd.river.task

import cd.river.persist.TaskQueue
import either.fold
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import java.net.URI

@Component
class TaskHandler(private val taskQueue: TaskQueue) {
    fun handle(req: ServerRequest): Mono<ServerResponse> {
        fun handleError(e: String) = status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BodyInserters.fromObject("Failed to add task to queue!\n" + e + "\n"))

        fun handleSuccess() = created(URI("test.com")).body(BodyInserters.fromObject("Passed"))

        return req.body(BodyExtractors.toMono(Task::class.java)).flatMap { t ->
            taskQueue.push(t).flatMap { either ->
                either.fold(
                        { e -> handleError(e) },
                        { _ -> handleSuccess() }
                )
            }
        }
    }
}