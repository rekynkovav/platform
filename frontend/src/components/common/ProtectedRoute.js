import React from 'react';
import { Navigate } from 'react-router-dom';
import { usePermissions } from '../../hooks/usePermissions';

/**
 * Компонент-обёртка для маршрутов, требующих определённый уровень доступа.
 * Если уровень пользователя ниже requiredLevel, перенаправляет на главную.
 */
const ProtectedRoute = ({ children, requiredLevel = 1 }) => {
    const { level } = usePermissions();

    if (level < requiredLevel) {
        return <Navigate to="/" replace />;
    }

    return children;
};

export default ProtectedRoute;
