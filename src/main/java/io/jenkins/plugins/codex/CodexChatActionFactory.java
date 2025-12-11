package io.jenkins.plugins.codex;

import hudson.Extension;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Factory that automatically adds CodexChatAction to every build.
 * This makes the "Codex Chat" option appear in the build dropdown menu.
 */
@Extension
public class CodexChatActionFactory extends TransientActionFactory<Run> {

    @Override
    public Class<Run> type() {
        return Run.class;
    }

    @Override
    public Collection<? extends CodexChatAction> createFor(Run target) {
        return Collections.singleton(new CodexChatAction(target));
    }
}
