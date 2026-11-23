
package varsystem.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Sends password-reset OTP messages through Gmail SMTP using a Gmail
 * App Password supplied through environment variables. No credentials
 * are stored in source code or in the serialized application database.
 */
public final class EmailOtpService {

    private EmailOtpService() { }

    public static boolean isConfigured() {
        String email = System.getenv("VAARL_SMTP_EMAIL");
        String password = System.getenv("VAARL_SMTP_APP_PASSWORD");
        return email != null && !email.isBlank()
                && password != null && !password.isBlank();
    }

    public static boolean sendOtp(String recipient, String otp) {

        String sender = System.getenv("VAARL_SMTP_EMAIL");
        String appPassword = System.getenv("VAARL_SMTP_APP_PASSWORD");

        if (sender == null || sender.isBlank()
                || appPassword == null || appPassword.isBlank()
                || recipient == null || recipient.isBlank()) {
            return false;
        }

        String host = valueOrDefault(
                System.getenv("VAARL_SMTP_HOST"), "smtp.gmail.com");
        int port = parsePort(System.getenv("VAARL_SMTP_PORT"), 465);

        try (SSLSocket socket = (SSLSocket)
                SSLSocketFactory.getDefault().createSocket(host, port)) {

            socket.setSoTimeout(15000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(), StandardCharsets.UTF_8));

            expect(readResponse(reader), 220);

            command(writer, reader, "EHLO vaarl.local", 250);
            command(writer, reader, "AUTH LOGIN", 334);
            command(writer, reader, Base64.getEncoder().encodeToString(
                    sender.getBytes(StandardCharsets.UTF_8)), 334);
            command(writer, reader, Base64.getEncoder().encodeToString(
                    appPassword.replace(" ", "").getBytes(StandardCharsets.UTF_8)), 235);
            command(writer, reader, "MAIL FROM:<" + sender + ">", 250);
            command(writer, reader, "RCPT TO:<" + recipient + ">", 250, 251);
            command(writer, reader, "DATA", 354);

            String body = "Your VAARL password reset verification code is: "
                    + otp + "\n\nThis code expires in 5 minutes. "
                    + "Do not share this code with anyone. If you did not "
                    + "request a password reset, you can ignore this email.";

            String encodedBody = Base64.getMimeEncoder(76,
                    "\r\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(body.getBytes(StandardCharsets.UTF_8));

            writer.write("From: VAARL System <" + sender + ">\r\n");
            writer.write("To: <" + recipient + ">\r\n");
            writer.write("Subject: VAARL Password Reset Verification Code\r\n");
            writer.write("MIME-Version: 1.0\r\n");
            writer.write("Content-Type: text/plain; charset=UTF-8\r\n");
            writer.write("Content-Transfer-Encoding: base64\r\n\r\n");
            writer.write(encodedBody);
            writer.write("\r\n.\r\n");
            writer.flush();

            expect(readResponse(reader), 250);
            command(writer, reader, "QUIT", 221);
            return true;

        } catch (IOException | RuntimeException ex) {
            System.err.println("VAARL OTP email could not be sent: "
                    + ex.getMessage());
            return false;
        }
    }

    private static void command(
            BufferedWriter writer,
            BufferedReader reader,
            String command,
            int expectedCode) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        expect(readResponse(reader), expectedCode);
    }

    private static void command(
            BufferedWriter writer,
            BufferedReader reader,
            String command,
            int expectedCodeA,
            int expectedCodeB) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        int code = readResponse(reader);
        if (code != expectedCodeA && code != expectedCodeB) {
            throw new IOException("SMTP server rejected a command (code "
                    + code + ").");
        }
    }

    private static int readResponse(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null || line.length() < 3) {
            throw new SocketTimeoutException("No valid SMTP response.");
        }

        int code;
        try {
            code = Integer.parseInt(line.substring(0, 3));
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid SMTP response.", ex);
        }

        while (line.length() > 3 && line.charAt(3) == '-') {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("Incomplete SMTP response.");
            }
        }

        return code;
    }

    private static void expect(int actual, int expected)
            throws IOException {
        if (actual != expected) {
            throw new IOException("SMTP server returned code " + actual
                    + "; expected " + expected + ".");
        }
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int parsePort(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
