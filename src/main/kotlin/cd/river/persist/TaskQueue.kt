package cd.river.persist

import cd.river.RiverProperties
import cd.river.task.Task
import com.fasterxml.jackson.annotation.JsonProperty
import either.Either
import either.Left
import either.Right
import org.springframework.data.redis.connection.ReactiveRedisConnection
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.ByteBuffer
import java.util.*

const val TASK_QUEUE_KEY: String = "RiverTaskQueue"
const val QUEUE_LIMIT_REACHED_MSG = "Queue size exceeded!"

@Component
class TaskQueue(reactiveRedisConnection: ReactiveRedisConnection, private val riverProperties: RiverProperties) {
    private final val stringSerializer = StringRedisSerializer()
    private final val jsonSerializer = Jackson2JsonRedisSerializer<TaskEntity>(TaskEntity::class.java)
    private final val listOps = reactiveRedisConnection.listCommands()
    private final val serializedTask = serialize(stringSerializer, TASK_QUEUE_KEY)

    data class TaskEntity(@JsonProperty("id") val id: UUID, @JsonProperty("label") val label: String)

    fun push(task: Task): Mono<Either<String, UUID>> {
        return listOps.lLen(serializedTask).flatMap { size ->
            if (size < riverProperties.queueLimit) {
                val id = UUID.randomUUID()
                val entity = TaskEntity(id, task.label)
                val toPersist = listOf(serialize(jsonSerializer, entity))
                listOps.lPush(serializedTask, toPersist).map { _ -> Right(id) }
            } else {
                Left(QUEUE_LIMIT_REACHED_MSG).toMono()
            }
        }
    }

    private final fun <T> serialize(serializer: RedisSerializer<T>, value: T): ByteBuffer {
        return ByteBuffer.wrap(serializer.serialize(value))
    }
}