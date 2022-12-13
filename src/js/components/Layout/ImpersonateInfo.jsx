import React from 'react';

import PropTypes from 'prop-types';
import { RiShieldUserLine } from 'react-icons/all';
import { connect } from 'react-redux';

import Button from 'components/form-elements/Button';
import Translate from 'utils/Translate';

const ImpersonateInfo = ({ currentUserName }) => (
  <div className="impersonate-box d-flex justify-content-between align-items-center">
    <div className="info d-flex align-items-center">
      <RiShieldUserLine />
      <span>
        <Translate id="react.default.impersonate.label" defaultMessage="You are impersonating user" />&nbsp;
        <span className="font-weight-bold">{currentUserName}</span>
      </span>
    </div>
    <a href="/openboxes/auth/logout">
      <Button defaultLabel="Logout" label="react.default.logout.label" />
    </a>
  </div>
);

const mapStateToProps = state => ({
  currentUserName: state.session.user.username,
});

export default connect(mapStateToProps)(ImpersonateInfo);

ImpersonateInfo.propTypes = {
  currentUserName: PropTypes.string.isRequired,
};