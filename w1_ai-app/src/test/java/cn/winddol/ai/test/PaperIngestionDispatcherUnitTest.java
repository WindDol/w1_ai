package cn.winddol.ai.test;

import cn.winddol.ai.paper.api.IPaperIngestJobRepository;
import cn.winddol.ai.paper.internal.PaperIngestionDispatcher;
import cn.winddol.ai.paper.internal.PaperIngestionWorkflow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaperIngestionDispatcherUnitTest {

    private static final String JOB_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void reservesJobBeforePuttingItOnExecutorQueue() {
        Fixture fixture = new Fixture();
        when(fixture.jobRepository.tryAcquire(eq(JOB_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(true);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        fixture.dispatcher.dispatch(JOB_ID);

        verify(fixture.executor).execute(taskCaptor.capture());
        taskCaptor.getValue().run();
        verify(fixture.workflow).process(eq(JOB_ID), anyString());
    }

    @Test
    void duplicateDispatchDoesNotEnterExecutorQueue() {
        Fixture fixture = new Fixture();
        when(fixture.jobRepository.tryAcquire(eq(JOB_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);

        fixture.dispatcher.dispatch(JOB_ID);

        verify(fixture.executor, never()).execute(any(Runnable.class));
        verify(fixture.workflow, never()).process(anyString(), anyString());
    }

    @Test
    void executorRejectionReleasesDispatchReservation() {
        Fixture fixture = new Fixture();
        when(fixture.jobRepository.tryAcquire(eq(JOB_ID), anyString(), any(LocalDateTime.class)))
                .thenReturn(true);
        doThrow(new TaskRejectedException("queue full"))
                .when(fixture.executor).execute(any(Runnable.class));

        assertThrows(TaskRejectedException.class, () -> fixture.dispatcher.dispatch(JOB_ID));

        verify(fixture.jobRepository).release(eq(JOB_ID), anyString());
    }

    private static class Fixture {
        private final TaskExecutor executor = mock(TaskExecutor.class);
        private final PaperIngestionWorkflow workflow = mock(PaperIngestionWorkflow.class);
        private final IPaperIngestJobRepository jobRepository = mock(IPaperIngestJobRepository.class);
        private final PaperIngestionDispatcher dispatcher = new PaperIngestionDispatcher(
                executor, workflow, jobRepository, 120);
    }
}
