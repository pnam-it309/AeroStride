package com.example.be;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Foundation for Unit Testing Services and Utils.
 * Automatically enables Mockito annotations (@Mock, @InjectMocks).
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceTest {
    // Shared unit test setup can be added here
}
