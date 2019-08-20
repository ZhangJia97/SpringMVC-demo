package xyz.suiwo.action.service.impl;

import xyz.suiwo.action.service.QueryService;
import xyz.suiwo.mvcframework.annotation.SWService;

@SWService
public class QueryServiceImpl implements QueryService {

    @Override
    public String get() {
        return "This is QueryServiceImpl";
    }
}
