import React from 'react';

import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { Tooltip } from 'react-tippy';

const TableCell = ({
  value, children, tooltip, tooltipLabel, link, reactLink, defaultValue, className,
}) => {
  let cellValue = children || value || defaultValue;

  if (tooltip) {
    cellValue = (
      <Tooltip
        arrow="true"
        delay="150"
        duration="250"
        hideDelay="50"
        html={tooltipLabel || value}
      >
        {cellValue}
      </Tooltip>
    );
  }
  const elementClasses = `${className} text-overflow-ellipsis`;

  let cellElement = <div className={elementClasses}>{cellValue}</div>;

  if (link && typeof link === 'string') {
    if (reactLink) {
      cellElement = <Link className={elementClasses} to={link}>{cellValue}</Link>;
    } else {
      cellElement = <a className={elementClasses} href={link}>{cellValue}</a>;
    }
  }

  return cellElement;
};

TableCell.defaultProps = {
  defaultValue: undefined,
  className: '',
  reactLink: false,
};

TableCell.propTypes = {
  link: PropTypes.string,
  reactLink: PropTypes.bool,
  className: PropTypes.string,
  tooltip: PropTypes.bool,
  tooltipLabel: PropTypes.string,
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.node),
    PropTypes.node,
  ]),
  defaultValue: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
  ]),
};


export default TableCell;
