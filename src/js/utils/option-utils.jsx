import _ from 'lodash';
import queryString from 'query-string';

import apiClient from 'utils/apiClient';


export const debounceUsersFetch = (waitTime, minSearchLength) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get('/openboxes/api/persons', { params: { name: searchTerm, status: true } })
        .then(result => callback(_.map(result.data.data, obj => (
          {
            ...obj,
            value: obj.id,
            label: obj.name,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceLocationsFetch = (
  waitTime,
  minSearchLength,
  activityCodes,
  fetchAll = false,
  withOrgCode = false,
  withTypeDescription = true,
  isReturnOrder = false,
) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      const activityCodesParams = activityCodes ? activityCodes.map(activityCode => `&activityCodes=${activityCode}`).join('') : '';
      const { direction } = queryString.parse(window.location.search);
      const directionParam = fetchAll ? null : direction;
      apiClient.get(`/openboxes/api/locations?name=${searchTerm}${directionParam ? `&direction=${directionParam}` : ''}${activityCodesParams}${isReturnOrder ? '&isReturnOrder=true' : ''}`)
        .then(result => callback(_.map(result.data.data, (obj) => {
          const locationType = withTypeDescription ? ` [${obj.locationType.description}]` : '';
          const label = `${obj.name}${locationType}`;
          return {
            id: obj.id,
            type: obj.locationType.locationTypeCode,
            name: obj.name,
            label: withOrgCode ? `${obj.organizationCode ? `${obj.organizationCode} - ` : ''}${label}` : label,
          };
        })))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceGlobalSearch = (waitTime, minSearchLength) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/json/globalSearch?term=${searchTerm}`)
        .then(result => callback(_.map(result.data, obj => (
          {
            value: obj.url,
            url: obj.url,
            label: obj.label,
            color: obj.color,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceProductsFetch = (waitTime, minSearchLength, locationId) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/api/products/search?name=${searchTerm}&productCode=${searchTerm}&location.id=${locationId}`)
        .then(result => callback(_.map(result.data.data, obj => (
          {
            value: obj.id,
            id: obj.id,
            name: obj.name,
            productCode: obj.productCode,
            handlingIcons: obj.handlingIcons,
            lotAndExpiryControl: obj.lotAndExpiryControl,
            label: `${obj.productCode} - ${obj.name}`,
            color: obj.color,
            exactMatch: obj.exactMatch,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceAvailableItemsFetch = (waitTime, minSearchLength) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/api/products/search?name=${searchTerm}&productCode=${searchTerm}&availableItems=true`)
        .then(result => callback(_.map(result.data.data, obj => (
          {
            id: obj.id,
            value: obj.id,
            name: obj.name,
            productCode: obj.productCode,
            quantityAvailable: obj.quantityAvailable,
            minExpirationDate: obj.minExpirationDate,
            handlingIcons: obj.product.handlingIcons,
            label: `${obj.productCode} - ${obj.name}`,
            color: obj.color,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceProductsInOrders = (waitTime, minSearchLength, vendor, destination) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/api/combinedShipmentItems/getProductsInOrders?name=${searchTerm}&vendor=${vendor}&destination=${destination}`)
        .then(result => callback(_.map(result.data.data, obj => (
          {
            value: obj.id,
            id: obj.id,
            name: obj.name,
            productCode: obj.productCode,
            handlingIcons: obj.handlingIcons,
            label: `${obj.productCode} - ${obj.name}`,
            color: obj.color,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceOrganizationsFetch = (waitTime, minSearchLength, roleTypes = ['ROLE_SUPPLIER'], active = false) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/api/organizations?q=${searchTerm}${roleTypes ? roleTypes.map(roleType => `&roleType=${roleType}`).join('') : ''}&active=${active}`)
        .then(result => callback(_.map(result.data.data, obj => (
          {
            value: obj.id,
            id: obj.id,
            name: obj.name,
            label: `${obj.code} ${obj.name}`,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const debounceLocationGroupsFetch = (waitTime, minSearchLength) =>
  _.debounce((searchTerm, callback) => {
    if (searchTerm && searchTerm.length >= minSearchLength) {
      apiClient.get(`/openboxes/api/locationGroups?q=${searchTerm}`)
        .then(result => callback(_.map(result.data.data, obj => (
          {
            id: obj.id,
            value: obj.id,
            name: obj.name,
            label: `${obj.name}`,
          }
        ))))
        .catch(() => callback([]));
    } else {
      callback([]);
    }
  }, waitTime);

export const organizationsFetch = (roleTypes = ['ROLE_SUPPLIER'], active = false) =>
  apiClient.get(`/openboxes/api/organizations?${roleTypes ? roleTypes.map(roleType => `&roleType=${roleType}`).join('') : ''}&active=${active}`)
    .then((res) => {
      if (res.data.data) {
        return res.data.data.map(obj => (
          {
            id: obj.id,
            value: obj.id,
            name: obj.name,
            label: `${obj.name}`,
          }
        ));
      }
      return null;
    });

export const fetchUserById = async (id) => {
  const response = await apiClient(`/openboxes/api/generic/person/${id}`);
  return response.data?.data;
};

export const fetchLocationById = async (id) => {
  const response = await apiClient(`/openboxes/api/locations/${id}`);
  return response.data?.data;
};

export const fetchProductsCategories = async () => {
  const response = await apiClient.get('/openboxes/api/categoryOptions');
  return response.data.data;
};

export const fetchProductsCatalogs = async () => {
  const response = await apiClient.get('/openboxes/api/catalogOptions');
  return response.data.data;
};

export const fetchProductsTags = async () => {
  const response = await apiClient.get('/openboxes/api/tagOptions');
  return response.data.data;
};
