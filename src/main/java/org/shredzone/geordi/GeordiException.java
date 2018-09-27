/*
 * geordi
 *
 * Copyright (C) 2018 Richard "Shred" Körber
 *   https://github.com/shred/geordi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.shredzone.geordi;

/**
 * A generic exception that is thrown when Geordi discovered an error while reading
 * sensors.
 */
public class GeordiException extends RuntimeException {
    private static final long serialVersionUID = -2458855658190084893L;

    public GeordiException(String msg) {
        super(msg);
    }

    public GeordiException(Throwable cause) {
        super(cause);
    }

    public GeordiException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
