package com.co.jarvis.service;

import java.io.Serializable;
import java.util.List;

public interface BaseService<D extends Serializable> {

    List<D> findAll();

    D findById(String id);

    D save(D dto);

    void deleteById(String id);

    D update(D dto, String id);
}
