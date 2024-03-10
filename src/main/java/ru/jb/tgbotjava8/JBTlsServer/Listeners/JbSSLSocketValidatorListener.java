package ru.jb.tgbotjava8.JBTlsServer.Listeners;

import ru.jb.tgbotjava8.JBTlsServer.JbSSLSocketValidator;

public interface JbSSLSocketValidatorListener {
    public void onValidateResult(boolean validationResult, JbSSLSocketValidator jbSSLSocketValidator);
}
