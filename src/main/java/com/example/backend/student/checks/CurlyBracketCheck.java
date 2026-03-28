package com.example.backend.student.checks;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

/**
 * Checkstyle listener that collects curly-bracket related violations.
 */
public class CurlyBracketCheck implements AuditListener {
    private final ViolationCollector data;

    public CurlyBracketCheck() {
        this.data = new ViolationCollector();
    }

    public ViolationCollector getViolationData() {
        return data;
    }

    @Override
    public void addError(AuditEvent event) {
        String source = event.getSourceName();
        if (source == null) {
            return;
        }

        String checkName = null;
        if (source.contains("LeftCurly")) {
            checkName = "LeftCurlyCheck";
        } else if (source.contains("RightCurly")) {
            checkName = "RightCurlyCheck";
        } else if (source.contains("NeedBraces")) {
            checkName = "NeedBracesCheck";
        } else if (source.contains("WhitespaceAround")) {
            checkName = "WhitespaceAroundCheck";
        }

        if (checkName != null) {
            data.addViolation(event.getLine(), event.getMessage(), checkName);
            data.addDeduction(1.0);
        }
    }

    @Override
    public void auditStarted(AuditEvent e) {
        data.setProjectName(System.getProperty("user.dir"));
    }

    @Override public void auditFinished(AuditEvent e) { }

    @Override public void fileStarted(AuditEvent e) { }

    @Override public void fileFinished(AuditEvent e) { }

    @Override public void addException(AuditEvent e, Throwable t) { }
}
