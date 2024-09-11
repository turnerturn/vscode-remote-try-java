package com.mycompany.app;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;




@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ScheduledPasswordRotationWatcherTest {

    @Mock
    private PasswordFetcher passwordFetcher;

    @Mock
    private Cryptor cryptor;

    @Mock
    private PasswordRotationRecovery passwordRotationRecovery;

    @InjectMocks
    private ScheduledPasswordRotationWatcher scheduledPasswordRotationWatcher;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testExecutePasswordRotationDetected() throws Exception {
        String password = "password";
        String encodedPassword = "encodedPassword";
        String newEncodedPassword = "newEncodedPassword";

        when(passwordFetcher.fetch()).thenReturn(password);
        when(cryptor.encode(password, anyString())).thenReturn(encodedPassword, newEncodedPassword);

        scheduledPasswordRotationWatcher.execute();

        verify(passwordRotationRecovery, times(1)).execute();
    }

    @Test
    public void testExecutePasswordRotationNotDetected() throws Exception {
        String password = "password";
        String encodedPassword = "encodedPassword";

        when(passwordFetcher.fetch()).thenReturn(password);
        when(cryptor.encode(password,anyString())).thenReturn(encodedPassword, encodedPassword);

        scheduledPasswordRotationWatcher.execute();

        verify(passwordRotationRecovery, never()).execute();
    }
}