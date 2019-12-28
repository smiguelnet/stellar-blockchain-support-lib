package net.smiguel.stellar.support.service;

import net.smiguel.stellar.support.exception.ServiceException;
import net.smiguel.stellar.support.model.Configuration;

public interface ConfigurationService {
    Configuration getConfiguration() throws ServiceException;
}
