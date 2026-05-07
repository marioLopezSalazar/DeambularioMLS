package com.iesaguadulce.deambulario.utils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.lifecycle.LiveData;

import com.iesaguadulce.deambulario.viewmodel.DataViewModel;
import com.iesaguadulce.deambulario.viewmodel.GlobalUIViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * U10 --- Unit tests for the Global UI loading state management.
 * Tests the GlobalUIViewModel.updateLoadingState() method, ensuring that the
 * global loading indicator is shown if at least one ViewModel is synchronizing data.
 *
 * @author Mario López Salazar.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateLoadingStateTest {

    /*
     * MOCKS objects for VIEW MODEL:
     */
    @Mock
    private GlobalUIViewModel mockGlobalUI;
    @Mock
    private DataViewModel mockViewModel1;
    @Mock
    private DataViewModel mockViewModel2;
    @Mock
    private LiveData<Boolean> mockLiveDataTrue;
    @Mock
    private LiveData<Boolean> mockLiveDataFalse;


    /**
     * Prepares the behavior for the mocked LiveData objects.
     */
    @Before
    public void setUp() {
        when(mockLiveDataTrue.getValue()).thenReturn(true);
        when(mockLiveDataFalse.getValue()).thenReturn(false);
    }


    /**
     * U10_A --- Verifies that if at least one ViewModel is loading, the global state is shown.
     */
    @Test
    public void updateLoadingState_oneViewModelLoading_showsLoading() {

        // Setting up ViewModel1 to return IsLoading=false but ViewModel2 to return IsLoading=true:
        when(mockViewModel1.isLoading()).thenReturn(mockLiveDataFalse);
        when(mockViewModel2.isLoading()).thenReturn(mockLiveDataTrue);

        // Execution:
        UIUtils.updateLoadingState(Arrays.asList(mockViewModel1, mockViewModel2), mockGlobalUI);

        // TEST: It should call always showLoading() and never hideLoading():
        verify(mockGlobalUI).showLoading();
        verify(mockGlobalUI, never()).hideLoading();
    }


    /**
     * U10_B --- Verifies that if all ViewModels are NOT loading, the global state is hidden.
     */
    @Test
    public void updateLoadingState_allViewModelsNotLoading_hidesLoading() {

        // Setting up ViewModels to return all IsLoading=false:
        when(mockViewModel1.isLoading()).thenReturn(mockLiveDataFalse);
        when(mockViewModel2.isLoading()).thenReturn(mockLiveDataFalse);

        // Execution:
        UIUtils.updateLoadingState(Arrays.asList(mockViewModel1, mockViewModel2), mockGlobalUI);

        // TEST: It should call always hideLoading() and never showLoading():
        verify(mockGlobalUI).hideLoading();
        verify(mockGlobalUI, never()).showLoading();
    }


    /**
     * U10_C --- Verifies that an empty list of ViewModels hides the global loading state.
     */
    @Test
    public void updateLoadingState_emptyList_hidesLoading() {

        // Empty ViewModels list:
        List<DataViewModel> emptyList = new ArrayList<>();

        // Execution:
        UIUtils.updateLoadingState(emptyList, mockGlobalUI);

        // TEST: It should call always hideLoading() and never showLoading():
        verify(mockGlobalUI).hideLoading();
        verify(mockGlobalUI, never()).showLoading();
    }


    /**
     * U10_D --- Verifies that if a ViewModel has a null LiveData, it is ignored and hides loading.
     */
    @Test
    public void updateLoadingState_nullLiveData_isIgnoredAndHidesLoading() {

        // Setting up ViewModel as IsLoading=null:
        when(mockViewModel1.isLoading()).thenReturn(null);

        // Execution:
        UIUtils.updateLoadingState(List.of(mockViewModel1), mockGlobalUI);

        // TEST: It should call always hideLoading() and never showLoading():
        verify(mockGlobalUI).hideLoading();
        verify(mockGlobalUI, never()).showLoading();
    }






}