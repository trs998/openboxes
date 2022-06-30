import React, { Component } from 'react';

import PropTypes from 'prop-types';
import { LiveChatLoaderProvider } from 'react-live-chat-loader';
import { connect } from 'react-redux';

import PropTypes from 'prop-types';
import Translate from '../../utils/Translate';
import GlobalSearch from '../GlobalSearch';
import LocationChooser from '../location/LocationChooser';
import UserActionMenu from '../user/UserActionMenu';
import apiClient, { stringUrlInterceptor } from '../../utils/apiClient';
import GlobalSearch from 'components/GlobalSearch';
import LocationChooser from 'components/location/LocationChooser';
import SupportButton from 'components/support-button/SupportButton';
import UserActionMenu from 'components/user/UserActionMenu';
import apiClient from 'utils/apiClient';
import Translate from 'utils/Translate';


class Header extends Component {
  constructor(props) {
    super(props);
    this.state = {
      logoUrl: '',
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.logoUrl !== this.props.logoUrl) {
      this.setLogoUrl(nextProps.logoUrl);
    }
  }

  setLogoUrl(logoUrl) {
    this.setState({ logoUrl });
  }

  logoutImpersonatedUser = () => {
    const url = '/api/logout';

    apiClient.post(url)
      .then(() => {
        window.location = stringUrlInterceptor('/dashboard/index');
      });
  }

  render() {
    return (
      <div className="w-100">
        {this.props.isImpersonated ?
          <div className="d-flex notice">
            <div className="ml-1"><Translate id="react.default.impersonate.label" defaultMessage="You are impersonating user" /></div>
            <div className="ml-1"><b>{this.props.username}</b></div>
            <div className="ml-1">
              <a
                href="#"
                onClick={() => this.logoutImpersonatedUser()}
              >
                <Translate id="react.default.logout.label" defaultMessage="Logout" />
              </a>
            </div>
          </div> : null}
        <div className="d-flex align-items-center justify-content-between flex-wrap">
          <div className="logo-header">
            <a
              href={this.props.highestRole === 'Authenticated' ? '/openboxes/stockMovement/list?direction=INBOUND' : '/openboxes'}
              className="navbar-brand brand-name"
            >
              { this.state.logoUrl !== '' ?
                <img alt="Openboxes" src={this.state.logoUrl} onError={(e) => { e.target.onerror = null; e.target.src = 'https://openboxes.com/img/logo_30.png'; }} /> : null
            }
            </a>
            { this.props.logoLabel.trim() !== '' ? <span>{this.props.logoLabel} </span> : null }
          </div>
          <div className="d-flex flex-wrap">
            <GlobalSearch />
            <UserActionMenu />
            <LocationChooser />
            {
              this.props.isHelpScoutEnabled &&
              <LiveChatLoaderProvider provider="helpScout" providerKey={this.props.localizedHelpScoutKey}>
                <SupportButton text="react.default.button.help.label" />
              </LiveChatLoaderProvider>
            }
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = state => ({
  username: state.session.user.username,
  isImpersonated: state.session.isImpersonated,
  highestRole: state.session.highestRole,
  logoUrl: state.session.logoUrl,
  logoLabel: state.session.logoLabel,
  localizedHelpScoutKey: state.session.localizedHelpScoutKey,
  isHelpScoutEnabled: state.session.isHelpScoutEnabled,
});

export default connect(mapStateToProps)(Header);

Header.propTypes = {
  /** Active user's username */
  username: PropTypes.string.isRequired,
  /** Indicator if active user is impersonated */
  isImpersonated: PropTypes.bool.isRequired,
  /** Id of the current location */
  logoUrl: PropTypes.string.isRequired,
  /** Id of the current location */
  logoLabel: PropTypes.string.isRequired,
  highestRole: PropTypes.string.isRequired,
  localizedHelpScoutKey: PropTypes.string,
  isHelpScoutEnabled: PropTypes.bool,
};

Header.defaultProps = {
  localizedHelpScoutKey: '',
  isHelpScoutEnabled: false,
};
