
package varsystem.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import varsystem.data.Session;

public final class LocationService {

    private LocationService() {
    }

    public static class Location {

        public final double latitude;
        public final double longitude;
        public final double accuracy;

        public Location(double latitude, double longitude, double accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

        public String displayName() {
            return String.format(
                Locale.US,
                "%.6f, %.6f",
                latitude,
                longitude
            );
        }
    }

    public static Location getCurrentLocation() throws Exception {

        // Central preference guard: location must not be requested while OFF.
        if (Session.ME != null && !Session.ME.loc) {
            throw new IllegalStateException(
                    "Location Sharing is OFF in your account Preferences.");
        }

        String script = String.join("\n",

            "Add-Type -AssemblyName System.Runtime.WindowsRuntime",

            "$null = [Windows.Devices.Geolocation.Geolocator, Windows.Devices.Geolocation, ContentType=WindowsRuntime]",

            "$geo = New-Object Windows.Devices.Geolocation.Geolocator",

            "$geo.DesiredAccuracy = [Windows.Devices.Geolocation.PositionAccuracy]::High",

            "$operation = $geo.GetGeopositionAsync()",

            "$method = [System.WindowsRuntimeSystemExtensions].GetMethods() |",

            "Where-Object {",

            "    $_.Name -eq 'AsTask' -and",

            "    $_.IsGenericMethod -and",

            "    $_.GetParameters().Count -eq 1 -and",

            "    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'",

            "} | Select-Object -First 1",

            "if ($null -eq $method) { throw 'Windows Location API method not found.' }",

            "$method = $method.MakeGenericMethod([Windows.Devices.Geolocation.Geoposition])",

            "$task = $method.Invoke($null, [object[]]@($operation))",

            "$position = $task.GetAwaiter().GetResult()",

            "$coordinates = $position.Coordinate.Point.Position",

            "Write-Output ('LATITUDE=' + $coordinates.Latitude)",

            "Write-Output ('LONGITUDE=' + $coordinates.Longitude)",

            "Write-Output ('ACCURACY=' + $position.Coordinate.Accuracy)"
        );

        ProcessBuilder builder = new ProcessBuilder(
            "powershell.exe",
            "-NoProfile",
            "-STA",
            "-ExecutionPolicy",
            "Bypass",
            "-Command",
            script
        );

        builder.redirectErrorStream(true);

        Process process = builder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        if (!process.waitFor(35, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new Exception("Windows location request timed out.");
        }

        if (process.exitValue() != 0) {
            throw new Exception(output.toString());
        }

        String result = output.toString();

        double latitude = extract(result, "LATITUDE");
        double longitude = extract(result, "LONGITUDE");
        double accuracy = extract(result, "ACCURACY");

        if (latitude < -90 || latitude > 90
                || longitude < -180 || longitude > 180) {
            throw new Exception("Invalid location coordinates.");
        }

        return new Location(latitude, longitude, accuracy);
    }

    private static double extract(String output, String key)
            throws Exception {

        Pattern pattern = Pattern.compile(
            "(?m)^" + key + "=([-+]?[0-9]*\\.?[0-9]+)"
        );

        Matcher matcher = pattern.matcher(output);

        if (!matcher.find()) {
            throw new Exception(
                "Unable to read " + key + " from Windows Location API.\n"
                + output
            );
        }

        return Double.parseDouble(matcher.group(1));
    }
}
