import { resolveSmtpConfig } from "./config.js";

/**
 * Creates a Nodemailer SMTP transporter from runtime secrets/environment config.
 */
export async function createEmailTransporter(smtpConfig, { nodemailerLib = null } = {}) {
  if (!smtpConfig?.user || !smtpConfig?.pass) {
    throw new Error(
      "SMTP_USER and SMTP_PASS secrets are not configured. Set them via Firebase Secret Manager before deploying."
    );
  }
  const mailer = nodemailerLib || (await import("nodemailer")).default;
  return mailer.createTransport({
    host: smtpConfig.host,
    port: smtpConfig.port,
    secure: smtpConfig.secure,
    auth: {
      user: smtpConfig.user,
      pass: smtpConfig.pass
    }
  });
}

/**
 * Sends a single admin alert email via the configured SMTP transport.
 */
export async function sendAdminAlertEmail({
  subject,
  text,
  html,
  smtpConfig = resolveSmtpConfig(),
  transporter = null,
  nodemailerLib = null
} = {}) {
  if (!subject || (!text && !html)) {
    throw new Error("Alert email requires subject and body content.");
  }

  const activeTransporter =
    transporter || (await createEmailTransporter(smtpConfig, { nodemailerLib }));
  const info = await activeTransporter.sendMail({
    from: smtpConfig.from,
    to: smtpConfig.adminEmail,
    subject,
    text,
    html
  });

  return {
    messageId: info?.messageId || "",
    recipient: smtpConfig.adminEmail
  };
}
