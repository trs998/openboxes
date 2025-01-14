import React, { useMemo } from 'react';

import PropTypes from 'prop-types';
import { RiDownload2Line } from 'react-icons/all';

import DataTable, { TableCell } from 'components/DataTable';
import Button from 'components/form-elements/Button';
import useProductsListTableData from 'hooks/list-pages/product/useProductsListTableData';
import StatusIndicator from 'utils/StatusIndicator';
import Translate from 'utils/Translate';

const ProductsListTable = ({
  filterParams,
}) => {
  const {
    tableData,
    tableRef,
    loading,
    onFetchHandler,
    exportProducts,
  } = useProductsListTableData(filterParams);

  // Columns for react-table
  const columns = useMemo(() => [
    {
      Header: <Translate id="react.productsList.column.active.label" defaultMessage="Active" />,
      accessor: 'active',
      className: 'active-circle d-flex justify-content-center',
      headerClassName: 'header justify-content-center',
      maxWidth: 150,
      Cell: row =>
        (<StatusIndicator
          variant={row.original.active ? 'success' : 'danger'}
          status={row.original.active ? 'Active' : 'Inactive'}
        />),
    },
    {
      Header: <Translate id="react.productsList.column.code.label" defaultMessage="Code" />,
      accessor: 'productCode',
      className: 'active-circle d-flex justify-content-center',
      headerClassName: 'header justify-content-center',
      Cell: row => <TableCell {...row} link={`/openboxes/inventoryItem/showStockCard/${row.original.id}`} />,
      maxWidth: 150,
    },
    {
      Header: <Translate id="react.productsList.column.name.label" defaultMessage="Name" />,
      accessor: 'name',
      className: 'active-circle',
      headerClassName: 'header',
      sortable: false,
      Cell: row => <TableCell {...row} tooltip link={`/openboxes/inventoryItem/showStockCard/${row.original.id}`} />,
      minWidth: 200,
    },
    {
      Header: <Translate id="react.productsList.filters.category.label" defaultMessage="Category" />,
      accessor: 'category',
      Cell: row => <TableCell {...row} tooltip />,
    },
    {
      Header: <Translate id="react.productsList.column.updatedBy.label" defaultMessage="Updated by" />,
      accessor: 'updatedBy',
    },
    {
      Header: <Translate id="react.productsList.column.lastUpdated.label" defaultMessage="Last updated" />,
      accessor: 'lastUpdated',
      maxWidth: 200,
    },
  ], []);

  return (
    <div className="list-page-list-section">
      <div className="title-text p-3 d-flex justify-content-between align-items-center">
        <span>
          <Translate id="react.productsList.header.label" defaultMessage="Product list" />
          &nbsp;
          ({tableData.totalCount})
        </span>
        <div className="btn-group">
          <Button
            isDropdown
            defaultLabel="Export"
            label="react.default.button.export.label"
            variant="secondary"
            EndIcon={<RiDownload2Line />}
          />
          <div className="dropdown-menu dropdown-menu-right nav-item padding-8" aria-labelledby="dropdownMenuButton">
            <a href="#" className="dropdown-item" onClick={() => exportProducts(false)} role="button" tabIndex={0}>
              <Translate
                id="react.productsList.exportResults.label"
                defaultMessage="Export results"
              />
            </a>
            <a className="dropdown-item" onClick={() => exportProducts(true)} href="#">
              <Translate
                id="react.productsList.exportProducts.label"
                defaultMessage="Export Products"
              />
            </a>
            <a className="dropdown-item" onClick={() => exportProducts(false, true)} href="#">
              <Translate
                id="react.productsList.exportProductAttrs"
                defaultMessage="Export Product Attributes"
              />
            </a>
          </div>
        </div>
      </div>
      <DataTable
        manual
        sortable
        ref={tableRef}
        columns={columns}
        data={tableData.data}
        loading={loading}
        defaultPageSize={10}
        pages={tableData.pages}
        totalData={tableData.totalCount}
        onFetchData={onFetchHandler}
        className="mb-1"
        noDataText="No products match the given criteria"
      />
    </div>
  );
};

export default ProductsListTable;


ProductsListTable.propTypes = {
  filterParams: PropTypes.shape({}).isRequired,
};
