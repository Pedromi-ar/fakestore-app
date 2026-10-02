package com.tienda.session;

import com.tienda.model.Role;

public record Session(String user, Role role) { }
