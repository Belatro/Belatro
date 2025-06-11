// AdminApp.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { DataGrid } from '@mui/x-data-grid';
import { Box, Button, TextField } from '@mui/material';

export default function AdminApp() {
    const [jwt, setJwt]       = useState('');
    const [users, setUsers]   = useState([]);
    const [error, setError]   = useState('');
    const [pageSize, setPageSize] = useState(20);

    // 1) Login handler
    const handleLogin = async (e) => {
        e.preventDefault();
        const { username, password } = e.target.elements;
        try {
            const resp = await axios.post(
                'http://localhost:8080/api/auth/login',
                { username: username.value, password: password.value }
            );
            setJwt(resp.data.token);
            setError('');
        } catch {
            setError('Bad credentials');
        }
    };

    // 2) Fetch users once we have a token
    useEffect(() => {
        if (!jwt) return;
        axios.get(
            'http://localhost:8080/admin/users',
            { headers: { Authorization: `Bearer ${jwt}` } }
        )
            .then(r => setUsers(r.data))
            .catch(() => setError('Failed to load users'));
    }, [jwt]);

    // 3) Delete user
    const deleteUser = async (id) => {
        if (!window.confirm('Really delete this user?')) return;
        try {
            await axios.delete(
                `http://localhost:8080/admin/users/${id}`,
                { headers: { Authorization: `Bearer ${jwt}` } }
            );
            setUsers(users.filter(u => u.id !== id));
        } catch {
            alert('Deletion failed');
        }
    };

    // If not logged in, show login form
    if (!jwt) {
        return (
            <Box
                component="form"
                onSubmit={handleLogin}
                sx={{ maxWidth: 320, margin: '2rem auto', display: 'flex', flexDirection: 'column', gap: 2 }}
            >
                <h2>Admin Login</h2>
                {error && <Box sx={{ color: 'error.main' }}>{error}</Box>}
                <TextField name="username" label="Username" defaultValue="admin" required />
                <TextField name="password" label="Password" type="password" required />
                <Button type="submit" variant="contained">Log In</Button>
            </Box>
        );
    }

    // DataGrid columns
    const columns = [
        { field: 'username', headerName: 'Username', flex: 1 },
        { field: 'email',    headerName: 'Email',    flex: 1.5 },
        {
            field: 'roles', headerName: 'Roles', flex: 1,
            renderCell: ({ row }) => Array.isArray(row.roles)
                ? row.roles.join(', ')
                : ''
        },
        {
            field: 'deletionRequested',
            headerName: 'Deletion Requested',
            flex: 0.5,
            renderCell: ({ value }) => value ? '✅' : ''
        },
        {
            field: 'actions',
            headerName: 'Actions',
            sortable: false,
            width: 140,
            renderCell: ({ row }) => (
                <Button
                    variant="contained"
                    color={row.deletionRequested ? 'warning' : 'error'}
                    size="small"
                    onClick={() => deleteUser(row.id)}
                >
                    {row.deletionRequested ? 'Purge' : 'Delete'}
                </Button>
            ),
        },
    ];

    return (
        <Box
            sx={{
                p: 2,
                backgroundColor: '#2b322f',      // dark outer container
                borderRadius: 1,
            }}
        >
            <h1 style={{ color: '#fff' }}>Admin Dashboard</h1>
            {error && <Box sx={{ color: 'error.main', mb: 1 }}>{error}</Box>}

            <Box sx={{ height: '80vh', width: '100%' }}>
                <DataGrid
                    rows={users}
                    columns={columns}
                    getRowId={(row) => row.id}
                    pageSize={pageSize}
                    onPageSizeChange={(newSize) => setPageSize(newSize)}
                    rowsPerPageOptions={[10, 20, 50]}
                    pagination
                    disableSelectionOnClick
                    sx={{
                        // override the Top Container (where that blank white bar lives)
                        '& .MuiDataGrid-topContainer': {
                            backgroundColor: '#1f2a27 !important',
                        },
                        // also override the wrapper around your actual headers
                        '& .MuiDataGrid-columnHeaderWrapper': {
                            backgroundColor: '#1f2a27 !important',
                        },

                        // (your existing overrides below)
                        backgroundColor: '#324339 !important',
                        '& .MuiDataGrid-columnHeaders': {
                            backgroundColor: '#1f2a27 !important',
                            color: '#ffffff !important',
                        },
                        '& .MuiDataGrid-columnHeader, .MuiDataGrid-columnHeaderTitle': {
                            backgroundColor: '#1f2a27 !important',
                            color: '#ffffff !important',
                        },
                        '& .MuiDataGrid-cell': {
                            color: '#e0e0e0 !important',
                        },
                        '& .MuiDataGrid-row:nth-of-type(odd)': { backgroundColor: '#2b322f !important' },
                        '& .MuiDataGrid-row:nth-of-type(even)': { backgroundColor: '#324339 !important' },

                        // keep your footer dark and text visible
                        '& .MuiDataGrid-footerContainer': {
                            backgroundColor: '#1f2a27 !important',
                        },
                        '& .MuiTablePagination-root, .MuiTablePagination-root *': {
                            color: '#ffffff !important',
                        },
                    }}
                />
                />
            </Box>
        </Box>


    );
}
