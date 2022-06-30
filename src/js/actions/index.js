/* eslint no-param-reassign: ["error", { "props": false }] */
import _ from 'lodash';
import { addTranslationForLanguage } from 'react-localize-redux';

import {
  CHANGE_CURRENT_LOCALE,
  CHANGE_CURRENT_LOCATION,
  FETCH_BREADCRUMBS_CONFIG,
  FETCH_CONFIG,
  FETCH_CONFIG_AND_SET_ACTIVE,
  FETCH_CURRENCIES,
  FETCH_GRAPHS,
  FETCH_MENU_CONFIG,
  FETCH_NUMBERS,
  FETCH_ORGANIZATIONS,
  FETCH_REASONCODES,
  FETCH_SESSION_INFO,
  FETCH_USERS,
  HIDE_SPINNER,
  REMOVE_FROM_INDICATORS,
  REORDER_INDICATORS,
  RESET_INDICATORS,
  SET_ACTIVE_CONFIG,
  SHOW_SPINNER,
  TOGGLE_LOCATION_CHOOSER,
  TOGGLE_USER_ACTION_MENU,
  TRANSLATIONS_FETCHED,
  UPDATE_BREADCRUMBS_PARAMS,
} from 'actions/types';
import apiClient, { parseResponse } from 'utils/apiClient';

export function showSpinner() {
  return {
    type: SHOW_SPINNER,
    payload: true,
  };
}

export function hideSpinner() {
  return {
    type: HIDE_SPINNER,
    payload: false,
  };
}

export function showLocationChooser() {
  return {
    type: TOGGLE_LOCATION_CHOOSER,
    payload: true,
  };
}

export function showUserActions() {
  return {
    type: TOGGLE_USER_ACTION_MENU,
    payload: true,
  };
}

export function hideLocationChooser() {
  return {
    type: TOGGLE_LOCATION_CHOOSER,
    payload: false,
  };
}

export function hideUserActions() {
  return {
    type: TOGGLE_USER_ACTION_MENU,
    payload: false,
  };
}

export function fetchReasonCodes() {
  const url = '/api/reasonCodes';
  const request = apiClient.get(url);

  return {
    type: FETCH_REASONCODES,
    payload: request,
  };
}

export function fetchCurrencies() {
  const url = '/openboxes/api/unitOfMeasure/currencies';
  const request = apiClient.get(url);

  return {
    type: FETCH_CURRENCIES,
    payload: request,
  };
}

export function fetchOrganizations() {
  const url = '/openboxes/api/organizations';
  const request = apiClient.get(url);

  return {
    type: FETCH_ORGANIZATIONS,
    payload: request,
  };
}

export function fetchUsers() {
  const url = '/api/generic/person';
  const request = apiClient.get(url);

  return {
    type: FETCH_USERS,
    payload: request,
  };
}

export function fetchSessionInfo() {
  const url = '/api/getAppContext';
  const request = apiClient.get(url);

  return {
    type: FETCH_SESSION_INFO,
    payload: request,
  };
}

export function fetchMenuConfig() {
  const url = '/api/getMenuConfig';
  const request = apiClient.get(url);

  return {
    type: FETCH_MENU_CONFIG,
    payload: request,
  };
}

export function changeCurrentLocation(location) {
  return (dispatch) => {
    const url = `/api/chooseLocation/${location.id}`;

    return apiClient.put(url).then(() => {
      dispatch({
        type: CHANGE_CURRENT_LOCATION,
        payload: location,
      });
    });
  };
}

export function fetchTranslations(lang, prefix) {
  return (dispatch) => {
    const url = `/api/localizations?lang=${lang ||
      ''}&prefix=react.${prefix || ''}`;

    apiClient.get(url).then((response) => {
      const { messages, currentLocale } = parseResponse(response.data);

      dispatch(addTranslationForLanguage(messages, currentLocale));

      dispatch({
        type: TRANSLATIONS_FETCHED,
        payload: prefix,
      });
    });
  };
}

export function changeCurrentLocale(locale) {
  return (dispatch) => {
    const url = `/api/chooseLocale/${locale}`;

    apiClient.put(url).then(() => {
      dispatch({
        type: CHANGE_CURRENT_LOCALE,
        payload: locale,
      });
    });
  };
}

// New Dashboard

function getParameterList(params = '', locationId = '', userId = '') {
  const listFiltersSelected = [];
  const listValues = [];

  const dashboardKey = sessionStorage.getItem('dashboardKey');

  const pageConfig = JSON.parse(sessionStorage.getItem('pageConfig')) || {};

  if (!pageConfig[dashboardKey]) { pageConfig[dashboardKey] = {}; }

  let listParams = params === '' ? `locationId=${locationId}` : `${params}&locationId=${locationId}`;
  listParams += userId ? `&userId=${userId}` : '';

  // List of filter and category
  // filter[0] is the category
  // filter[1] represent the values
  Object.entries(pageConfig[dashboardKey]).forEach((filter) => {
    listFiltersSelected.push(filter[0]);
    filter[1].forEach(value => listValues.push(value));
  });
  // Add condition to check if currentPage has any filter
  listFiltersSelected.forEach((filter) => {
    listParams = `${listParams}&listFiltersSelected=${filter}`;
  });
  listValues.forEach((value) => {
    listParams = `${listParams}&value=${value.id}`;
  });

  return listParams;
}

function fetchGraphIndicator(
  dispatch,
  indicatorConfig,
  locationId = '',
  params = '',
) {
  const id = indicatorConfig.order;

  const listParams = getParameterList(params, locationId);
  const url = `${indicatorConfig.endpoint}?${listParams}`;

  dispatch({
    type: FETCH_GRAPHS,
    payload: {
      id,
      widgetId: indicatorConfig.widgetId,
      title: 'Loading...',
      info: 'Loading...',
      type: 'loading',
      data: [],
    },
  });

  apiClient.get(url).then((res) => {
    const indicatorData = res.data;
    dispatch({
      type: FETCH_GRAPHS,
      payload: {
        id,
        widgetId: indicatorConfig.widgetId,
        title: indicatorConfig.title,
        info: indicatorConfig.info,
        type: indicatorConfig.graphType,
        data: indicatorData.data,
        timeFilter: indicatorConfig.timeFilter,
        yearTypeFilter: indicatorConfig.yearTypeFilter,
        locationFilter: indicatorConfig.locationFilter,
        timeLimit: indicatorConfig.timeLimit,
        link: indicatorData.link,
        legend: indicatorConfig.legend,
        doubleAxeY: indicatorConfig.doubleAxeY,
        config: {
          stacked: indicatorConfig.stacked,
          datalabel: indicatorConfig.datalabel,
          colors: indicatorConfig.colors,
        },
        size: indicatorConfig.size,
      },
    });
  }, () => {
    dispatch({
      type: FETCH_GRAPHS,
      payload: {
        id,
        widgetId: indicatorConfig.widgetId,
        title: 'Indicator could not be loaded',
        type: 'error',
        data: [],
      },
    });
  });
}

function fetchNumberIndicator(
  dispatch,
  indicatorConfig,
  locationId,
  userId,
) {
  const id = indicatorConfig.order;

  const listParams = getParameterList('', locationId, userId);

  const url = `${indicatorConfig.endpoint}?${listParams}`;

  apiClient.get(url).then((res) => {
    const indicatorData = res.data;
    dispatch({
      type: FETCH_NUMBERS,
      payload: {
        ...indicatorData,
        id,
        widgetId: indicatorConfig.widgetId,
        title: indicatorConfig.title,
        info: indicatorConfig.info,
        subtitle: indicatorConfig.subtitle,
        numberType: indicatorConfig.numberType,
      },
    });
  });
}

export function reloadIndicator(indicatorConfig, params, locationId) {
  return (dispatch) => {
    // new reference so that the original config is not modified
    const indicatorConfigData = JSON.parse(JSON.stringify(indicatorConfig));
    fetchGraphIndicator(dispatch, indicatorConfigData, locationId, params);
  };
}

function getData(dispatch, dashboardConfig, locationId, config = 'personal', userId = '') {
  // new reference so that the original config is not modified

  const dashboard = dashboardConfig.dashboards[config] || {};
  const widgets = _.map(dashboard.widgets, widget => ({
    ...dashboardConfig.dashboardWidgets[widget.widgetId],
    order: widget.order,
    widgetId: widget.widgetId,
  }));

  const visibleWidgets = _.chain(widgets)
    .filter(widget => widget.enabled)
    .sortBy(['order']).value();

  _.forEach(visibleWidgets, (widgetConfig) => {
    if (widgetConfig.type === 'graph') {
      fetchGraphIndicator(dispatch, widgetConfig, locationId, '');
    } else if (widgetConfig.type === 'number') {
      fetchNumberIndicator(dispatch, widgetConfig, locationId, userId);
    }
  });
}

export function fetchIndicators(
  configData,
  config,
  locationId,
  userId,
) {
  return (dispatch) => {
    dispatch({
      type: SET_ACTIVE_CONFIG,
      payload: {
        data: config,
      },
    });

    getData(dispatch, configData, locationId, config, userId);
  };
}

export function resetIndicators() {
  return {
    type: RESET_INDICATORS,
  };
}

export function addToIndicators(widgetConfig, locationId, userId = '') {
  return (dispatch) => {
    if (widgetConfig.type === 'graph') {
      fetchGraphIndicator(dispatch, widgetConfig, locationId, '');
    } else if (widgetConfig.type === 'number') {
      fetchNumberIndicator(dispatch, widgetConfig, locationId, userId);
    }
  };
}

export function reorderIndicators({ oldIndex, newIndex }, e, type) {
  if (e.target.id === 'archive') {
    return {
      type: REMOVE_FROM_INDICATORS,
      payload: { index: oldIndex, type },
    };
  }
  return {
    type: REORDER_INDICATORS,
    payload: { oldIndex, newIndex, type },
  };
}

export function fetchConfigAndData(locationId, config = 'personal', userId, filterSelected) {
  return (dispatch) => {
    apiClient.get('/openboxes/api/dashboard/config').then((res) => {
      dispatch({
        type: FETCH_CONFIG_AND_SET_ACTIVE,
        payload: {
          data: res.data,
          activeConfig: config,
        },
      });
      getData(dispatch, res.data, locationId, config, userId, filterSelected);
    });
  };
}

export function fetchConfig() {
  return (dispatch) => {
    apiClient.get('/openboxes/api/dashboard/config').then((res) => {
      dispatch({
        type: FETCH_CONFIG,
        payload: {
          data: res.data,
        },
      });
    });
  };
}

function dispachBreadcrumbsParams(newData, dispatch) {
  dispatch({
    type: UPDATE_BREADCRUMBS_PARAMS,
    payload: newData,
  });
}

export function updateBreadcrumbs(listBreadcrumbsStep = [
  {
    label: null, defaultLabel: null, url: null, id: null,
  },
]) {
  return (dispatch) => {
    const breadcrumbsParams = [];
    listBreadcrumbsStep.forEach((step) => {
      breadcrumbsParams.push({
        label: step.label || '',
        defaultLabel: step.defaultLabel,
        url: step.id ? `${step.url}${step.id}` : step.url || '',
      });
    });
    dispachBreadcrumbsParams(breadcrumbsParams, dispatch);
  };
}

export function fetchBreadcrumbsConfig() {
  return (dispatch) => {
    apiClient.get('/openboxes/api/dashboard/breadcrumbsConfig').then((res) => {
      dispatch({
        type: FETCH_BREADCRUMBS_CONFIG,
        payload: res.data,
      });
    });
  };
}
