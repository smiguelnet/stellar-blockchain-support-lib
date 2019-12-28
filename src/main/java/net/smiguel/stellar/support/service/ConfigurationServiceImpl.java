package net.smiguel.stellar.support.service;

import net.smiguel.stellar.support.exception.ServiceException;
import net.smiguel.stellar.support.model.Configuration;

public class ConfigurationServiceImpl implements ConfigurationService {

    @Override
    public Configuration getConfiguration() throws ServiceException {
        //Used only for demonstration purpose
        return new Configuration();
    }
}
