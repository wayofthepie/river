package cd.river.persist

import cd.river.RiverProperties
import cd.river.task.Task
import com.nhaarman.mockito_kotlin.*
import either.Left
import either.Right
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.springframework.data.redis.connection.ReactiveListCommands
import org.springframework.data.redis.connection.ReactiveRedisConnection
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import reactor.core.publisher.toMono
import java.nio.ByteBuffer

class TaskQueueTest {
    private val limit: Long = 2
    private val stringSerializer = StringRedisSerializer()
    private val jsonSerializer = Jackson2JsonRedisSerializer<TaskQueue.TaskEntity>(TaskQueue.TaskEntity::class.java)

    private val listOpsMock = mock<ReactiveListCommands> {}
    private val reactiveRedisConnMock = mock<ReactiveRedisConnection> {
        on { listCommands() } doReturn listOpsMock
    }

    @Test
    @DisplayName("Verify push gives error if queue limit reached")
    fun pushQueueLimitReached() {
        val task = Task(label = "test")
        val taskQueue = initTaskQueue(reactiveRedisConnMock, RiverProperties(2L))
        whenever(listOpsMock.lLen(ByteBuffer.wrap(stringSerializer.serialize("RiverTaskQueue"))))
                .thenReturn(2L.toMono())

        taskQueue.push(task).subscribe { e ->
            assertThat(e).isEqualTo(Left("Queue size exceeded!"))
        }
    }

    @Test
    @DisplayName("Verify push returns the entities id on success")
    @Suppress("UNCHECKED_CAST")
    fun pushQueuePersistedTask() {
        // Arrange
        val label = "test"
        val taskToPersist = Task(label = label)
        val serializedTaskKey = ByteBuffer.wrap(stringSerializer.serialize("RiverTaskQueue"))
        val taskQueue = initTaskQueue(reactiveRedisConnMock, RiverProperties(3L))
        val captor: ArgumentCaptor<List<ByteBuffer>> = ArgumentCaptor.forClass(List::class.java as Class<List<ByteBuffer>>)
        
        whenever(listOpsMock.lLen(serializedTaskKey)).thenReturn(2L.toMono())
        whenever(listOpsMock.lPush(any(), any())).thenReturn(3L.toMono())

        // Assert
        taskQueue.push(taskToPersist).subscribe { either ->
            verify(listOpsMock).lPush(eq(serializedTaskKey), captor.capture())
            val buf = captor.value[0]
            val task = jsonSerializer.deserialize(buf.array())
            assertThat(task.label).isEqualTo(label)
            assertThat(either).isEqualTo(Right(task.id))
        }
    }

    private fun initTaskQueue(reactiveRedisConnection: ReactiveRedisConnection, props: RiverProperties): TaskQueue {
        return TaskQueue(reactiveRedisConnection, props)
    }
}