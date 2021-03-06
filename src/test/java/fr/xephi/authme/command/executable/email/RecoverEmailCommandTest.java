package fr.xephi.authme.command.executable.email;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.Duration;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link RecoverEmailCommand}.
 */
@RunWith(DelayedInjectionRunner.class)
public class RecoverEmailCommandTest {

    private static final String DEFAULT_EMAIL = "your@email.com";

    @InjectDelayed
    private RecoverEmailCommand command;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private CommonService commonService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private EmailService emailService;
    
    @Mock
    private RecoveryCodeService recoveryCodeService;

    @Mock
    private Messages messages;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void initSettings() {
        given(commonService.getProperty(SecuritySettings.EMAIL_RECOVERY_COOLDOWN_SECONDS)).willReturn(40);
    }

    @Test
    public void shouldHandleMissingMailProperties() {
        // given
        given(emailService.hasAllInformation()).willReturn(false);
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("some@email.tld"));

        // then
        verify(commonService).send(sender, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verifyZeroInteractions(dataSource, passwordSecurity);
    }

    @Test
    public void shouldShowErrorForAuthenticatedUser() {
        // given
        String name = "Bobby";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("bobby@example.org"));

        // then
        verify(emailService).hasAllInformation();
        verifyZeroInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    public void shouldShowRegisterMessageForUnregisteredPlayer() {
        // given
        String name = "Player123";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.USAGE_REGISTER);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldHandleInvalidEmailInput() {
        // given
        String name = "Rapt0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commonService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldGenerateRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendRecoveryCode(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "v@example.com";
        given(dataSource.getAuth(name)).willReturn(newAuthWithEmail(email));
        String code = "a94f37";
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.generateCode(name)).willReturn(code);

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase()));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        verify(recoveryCodeService).generateCode(name);
        verify(commonService).send(sender, MessageKey.RECOVERY_CODE_SENT);
        verify(emailService).sendRecoveryCode(name, email, code);
    }

    @Test
    public void shouldSendErrorForInvalidRecoveryCode() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.isCodeValid(name, "bogus")).willReturn(false);

        // when
        command.executeCommand(sender, Arrays.asList(email, "bogus"));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource, only()).getAuth(name);
        verify(commonService).send(sender, MessageKey.INCORRECT_RECOVERY_CODE);
        verifyNoMoreInteractions(emailService);
    }

    @Test
    public void shouldResetPasswordAndSendEmail() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        String code = "A6EF3AC8";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword(invocation.getArgument(0)));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.isCodeValid(name, code)).willReturn(true);

        // when
        command.executeCommand(sender, Arrays.asList(email, code));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordSecurity).computeHash(passwordCaptor.capture(), eq(name));
        String generatedPassword = passwordCaptor.getValue();
        assertThat(generatedPassword, stringWithLength(20));
        verify(dataSource).updatePassword(eq(name), any(HashedPassword.class));
        verify(recoveryCodeService).removeCode(name);
        verify(emailService).sendPasswordMail(name, email, generatedPassword);
        verify(commonService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    public void shouldGenerateNewPasswordWithoutRecoveryCode() {
        // given
        String name = "sh4rK";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendPasswordMail(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "shark@example.org";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commonService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(invocation -> new HashedPassword(invocation.getArgument(0)));
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(false);

        // when
        command.executeCommand(sender, Collections.singletonList(email));

        // then
        verify(emailService).hasAllInformation();
        verify(dataSource).getAuth(name);
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordSecurity).computeHash(passwordCaptor.capture(), eq(name));
        String generatedPassword = passwordCaptor.getValue();
        assertThat(generatedPassword, stringWithLength(20));
        verify(dataSource).updatePassword(eq(name), any(HashedPassword.class));
        verify(emailService).sendPasswordMail(name, email, generatedPassword);
        verify(commonService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }

    @Test
    public void shouldNotSendEmailIfCooldownCheckFails() {
        // given
        String name = "feverRay";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(emailService.hasAllInformation()).willReturn(true);
        given(emailService.sendRecoveryCode(anyString(), anyString(), anyString())).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "mymail@example.org";
        PlayerAuth auth = newAuthWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(recoveryCodeService.isRecoveryCodeNeeded()).willReturn(true);
        given(recoveryCodeService.generateCode(anyString())).willReturn("Code");
        // Trigger sending of recovery code
        command.executeCommand(sender, Collections.singletonList(email));

        Mockito.reset(emailService, commonService);
        given(emailService.hasAllInformation()).willReturn(true);
        given(messages.formatDuration(any(Duration.class))).willReturn("8 minutes");

        // when
        command.executeCommand(sender, Collections.singletonList(email));

        // then
        verify(emailService, only()).hasAllInformation();
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(messages).formatDuration(durationCaptor.capture());
        assertThat(durationCaptor.getValue().getDuration(), both(lessThan(41L)).and(greaterThan(36L)));
        assertThat(durationCaptor.getValue().getTimeUnit(), equalTo(TimeUnit.SECONDS));
        verify(messages).send(sender, MessageKey.EMAIL_COOLDOWN_ERROR, "8 minutes");
    }


    private static PlayerAuth newAuthWithEmail(String email) {
        return PlayerAuth.builder()
            .name("name")
            .email(email)
            .build();
    }
}
