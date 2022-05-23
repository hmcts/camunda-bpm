package uk.gov.hmcts.reform.camunda.bpm.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import feign.FeignException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.camunda.bpm.domain.response.ConfigureTaskResponse;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeignRetryPolicyTest {

    private static final int MAX_RETRIES = 3;

    private FeignRetryPolicy<ConfigureTaskResponse> withFeignRetryPolicy;

    private ListAppender<ILoggingEvent> listAppender;

    @Before
    public void setUp() {

        Logger logger = (Logger) LoggerFactory.getLogger(FeignRetryPolicy.class);
        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        withFeignRetryPolicy = spy(new FeignRetryPolicy<>(MAX_RETRIES));
    }

    @Test
    public void should_not_retry_when_successful() {

        ConfigureTaskResponse responseMock = mock(ConfigureTaskResponse.class);
        Supplier<ConfigureTaskResponse> methodCall = () -> responseMock;

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertEquals(result, responseMock);
        assertEquals(0, withFeignRetryPolicy.getRetryCount());

    }


    @Test
    public void should_catch_other_exceptions_not_retry_and_return_null() {

        Supplier<ConfigureTaskResponse> methodCall = () -> {
            throw new IllegalArgumentException("another exception");
        };

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertNull(result);
        assertEquals(0, withFeignRetryPolicy.getRetryCount());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(
            "Non retryable exception was received, call will be aborted. Exception message was: another exception",
            logsList.get(0).getFormattedMessage());
        assertEquals(Level.ERROR, logsList.get(0).getLevel());

    }

    @Test
    public void should_only_retry_once_and_succeed() {

        ConfigureTaskResponse responseMock = mock(ConfigureTaskResponse.class);

        FeignException exception = mock(FeignException.class);

        Supplier<ConfigureTaskResponse> methodCall = () -> {
            if (withFeignRetryPolicy.getRetryCount() == 0) {
                throw new RuntimeException(exception);
            } else {
                return responseMock;
            }
        };

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertEquals(result, responseMock);
        assertEquals(1, withFeignRetryPolicy.getRetryCount());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Call failed, The call will be retried 3 times.", logsList.get(0).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(0).getLevel());

        assertEquals("[1/3] - Call failed on retry.", logsList.get(1).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(1).getLevel());
    }

    @Test
    public void should_return_null_when_max_retries_is_reached() {

        FeignException exception = mock(FeignException.class);

        Supplier<ConfigureTaskResponse> methodCall = () -> {
            throw new RuntimeException(exception);
        };

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertNull(result);
        assertEquals(3, withFeignRetryPolicy.getRetryCount());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Call failed, The call will be retried 3 times.", logsList.get(0).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(0).getLevel());

        assertEquals("[1/3] - Call failed on retry.", logsList.get(1).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(1).getLevel());

        assertEquals("[2/3] - Call failed on retry.", logsList.get(2).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(2).getLevel());

        assertEquals("[3/3] - Call failed on retry.", logsList.get(3).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(3).getLevel());

        assertEquals("Maximum allowed retries exceeded.", logsList.get(4).getFormattedMessage());
        assertEquals(Level.ERROR, logsList.get(4).getLevel());
    }

    @Test
    public void should_return_null_when_non_retryable_exception_is_thrown() {

        FeignException exception = mock(FeignException.class);

        when(exception.status()).thenReturn(HttpStatus.NOT_FOUND.value());

        Supplier<ConfigureTaskResponse> methodCall = () -> {
            throw new RuntimeException(exception);
        };

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertNull(result);
        assertEquals(0, withFeignRetryPolicy.getRetryCount());

        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getFormattedMessage()
                .contains("Non retryable exception was received, call will be aborted. Exception message was:")
        );

        assertEquals(Level.ERROR, logsList.get(0).getLevel());

    }

    @Test
    public void should_retry_once_first_call_is_unsuccessful_and_then_non_retryable_exception() {

        FeignException exception = mock(FeignException.ServiceUnavailable.class);
        FeignException nonRetryableException = mock(FeignException.class);
        when(nonRetryableException.status()).thenReturn(HttpStatus.NOT_FOUND.value());

        Supplier<ConfigureTaskResponse> methodCall = () -> {
            if (withFeignRetryPolicy.getRetryCount() == 1) {
                throw new RuntimeException(nonRetryableException);
            } else {
                throw new RuntimeException(exception);
            }
        };

        ConfigureTaskResponse result = withFeignRetryPolicy.run(methodCall);

        assertNull(result);
        assertEquals(1, withFeignRetryPolicy.getRetryCount());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Call failed, The call will be retried 3 times.", logsList.get(0).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(0).getLevel());

        assertEquals("[1/3] - Call failed on retry.", logsList.get(1).getFormattedMessage());
        assertEquals(Level.WARN, logsList.get(1).getLevel());

        assertEquals(
            "Non retryable exception was received, call will be aborted.",
            logsList.get(2).getFormattedMessage()
        );
        assertEquals(Level.ERROR, logsList.get(2).getLevel());

    }

}
