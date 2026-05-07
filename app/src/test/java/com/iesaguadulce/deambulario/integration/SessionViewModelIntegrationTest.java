package com.iesaguadulce.deambulario.integration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.iesaguadulce.deambulario.TestUtils;
import com.iesaguadulce.deambulario.model.repository.SessionRepository;
import com.iesaguadulce.deambulario.model.repository.callback.ReposVoidCallback;
import com.iesaguadulce.deambulario.viewmodel.SessionViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Constructor;

/**
 * I1 --- Integration test for the MVVM Architecture.
 * Verifies the flow of data between an Observer (acting as StudentPlayFragment),
 * the SessionViewModel, and the singleton SessionRepository when a student sends an SOS alert.
 *
 * @author Mario López Salazar
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionViewModelIntegrationTest {

    // Indicating that, on testing, LiveData must execute synchronously:
    @Rule
    public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();


    /*
     * OBJECTS to perform the test:
     */
    private SessionViewModel viewModel;
    private final String sessionId = "SESSION_TEST_123";
    private final String studentNick = "mario_ls";
    private final Exception errorNetwork = new Exception("Firestore connection error");

    /*
     * MOCKS of repository and observer-fragment:
     */
    @Mock
    private SessionRepository mockedRepository;
    @Mock
    Observer<Boolean> observerIsLoading;
    @Mock
    Observer<Exception> observerGetError;

    /*
     * CAPTOR to analyze the callback used to communicate the sending result from repository to viewModel:
     */
    @Captor
    private ArgumentCaptor<ReposVoidCallback> callbackCaptor;



    /**
     * Initializes the Observer (it will act as the StudentPlayFragment),
     * and the SessionViewModel using OOP reflection.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {

        // Initializing the observers:
        observerIsLoading = Mockito.mock(Observer.class);
        observerGetError = Mockito.mock(Observer.class);

        // Getting the repository instance (singleton pattern):
        MockedStatic<SessionRepository> mockedRepoStatic = null;
        try {
            mockedRepoStatic = Mockito.mockStatic(SessionRepository.class);
            mockedRepoStatic.when(SessionRepository::getInstance).thenReturn(mockedRepository);

            // Calling the SessionViewModel constructor (using reflection, because the constructor is declared private):
            Constructor<SessionViewModel> constructor = SessionViewModel.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            viewModel = constructor.newInstance();
        } finally {
            // Freeing static mock:
            if (mockedRepoStatic != null) {
                mockedRepoStatic.close();
            }
        }

        // Mocking the SessionRepository connected to the SessionViewModel:
        TestUtils.setField(viewModel, "repository", mockedRepository);
    }


    /**
     * I1_A --- Verifies an alert sending flow with Success response from Firebase API.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void sendAlert_integratesWithRepository_success() {

        // The Observers start listening to the ViewModel liveData:
        viewModel.isLoading().observeForever(observerIsLoading);
        viewModel.getError().observeForever(observerGetError);
        // Consuming the on-start-listening first observation:
        Mockito.clearInvocations(observerIsLoading);
        Mockito.clearInvocations(observerGetError);

        // Sending the alert:
        viewModel.sendAlert(sessionId, studentNick);

        // TEST: The fragment-observer has received loading=true:
        verify(observerIsLoading).onChanged(true);

        // TEST: The repository launched the sendAlertMessage with the correct values (also capturing the callback passed to that method):
        verify(mockedRepository).sendAlertMessage(eq(sessionId), eq(studentNick), callbackCaptor.capture());

        // KEY: Simulating a OnSuccess value from Firebase:
        callbackCaptor.getValue().onSuccess();

        // TEST: The fragment-observer has received loading=false and error=null:
        verify(observerIsLoading).onChanged(false);
        verify(observerGetError, times(0)).onChanged(null);

        // The observers stop listening to the ViewModel liveData:
        viewModel.isLoading().removeObserver(observerIsLoading);
        viewModel.getError().removeObserver(observerGetError);
    }


    /**
     * I1_B --- Verifies an alert sending flow with Error response from Firebase API.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void sendAlert_integratesWithRepository_nonSuccess() {

        // The Observers start listening to the ViewModel liveData:
        viewModel.isLoading().observeForever(observerIsLoading);
        viewModel.getError().observeForever(observerGetError);
        // Consuming the on-start-listening first observation:
        Mockito.clearInvocations(observerIsLoading);
        Mockito.clearInvocations(observerGetError);

        // Sending the alert:
        viewModel.sendAlert(sessionId, studentNick);

        // TEST: The fragment-observer has received loading=true:
        verify(observerIsLoading).onChanged(true);

        // TEST: The repository launched the sendAlertMessage with the correct values (also capturing the callback passed to that method):
        verify(mockedRepository).sendAlertMessage(eq(sessionId), eq(studentNick), callbackCaptor.capture());

        // KEY: Simulating a OnError value from Firebase:
        callbackCaptor.getValue().onError(errorNetwork);

        // TEST: The fragment-observer has received loading=false and error NOT null::
        verify(observerIsLoading).onChanged(false);
        verify(observerGetError).onChanged(errorNetwork);

        // The observers stop listening to the ViewModel liveData:
        viewModel.isLoading().removeObserver(observerIsLoading);
        viewModel.getError().removeObserver(observerGetError);
    }

}