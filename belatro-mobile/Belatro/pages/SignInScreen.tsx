import React, { useState } from 'react';
import { SafeAreaView, View, Text, TextInput, TouchableOpacity, Alert, StyleSheet } from 'react-native';
import { loginUser } from '../services/userService';
import styles from '../styles/styles';
import { useAuth } from '../context/authContext';

export default function SignInScreen({ navigation }: { navigation: any }) {
    const { login } = useAuth();
    const [form, setForm] = useState({
        username: '',
        password: '',
    });
    const [isLoading, setIsLoading] = useState(false);

    const validate = () => {
        if (!form.username.trim()) {
            Alert.alert('Validation', 'Username is required');
            return false;
        }
        if (!form.password) {
            Alert.alert('Validation', 'Password is required');
            return false;
        }
        return true;
    };

    const handleSignIn = async () => {
        if (!validate()) return;
        setIsLoading(true);
        try {
            const response = await loginUser({
                username: form.username,
                password: form.password
            });
            const { token, user } = response.data;
            console.log('Login response:', response.data);
            console.log('Token:', token);
            console.log('User:', user);
            await login({
                id: user.id,
                username: user.username,
                email: user.email,
                token: token,
            }, token);
            Alert.alert('Success', 'Successfully logged in!', [
                {
                    text: 'OK', onPress: () => navigation.navigate('Home', {
                        userId: user.id,
                        username: user.username,
                        token: token
                    })
                },
            ]);
        } catch (error: any) {
            console.error('Login error:', error);
            let message = 'Login failed. Please try again.';
            if (error.response) {
                if (error.response.status === 403) {
                    message = 'Access denied. Please check your username and password.';
                } else if (error.response.data?.message) {
                    message = error.response.data.message;
                }
            } else if (error.request) {
                message = 'Unable to connect to the server. Please check your network.';
            } else {
                message = error.message || 'An unexpected error occurred.';
            }
            Alert.alert('Error', message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <SafeAreaView style={{ flex: 1, backgroundColor: '#1e1e1e' }}>
            <View style={styles.container}>
                <View style={styles.header}>
                    <Text style={styles.loginText}>Sign in to Belatro</Text>
                </View>
                <View style={styles.form}>
                    <View style={styles.input}>
                        <Text style={styles.inputLabel}>Username</Text>
                        <TextInput
                            autoCapitalize="none"
                            autoCorrect={false}
                            style={styles.inputControl}
                            value={form.username}
                            onChangeText={(username) => setForm({ ...form, username })}
                        />
                    </View>
                    <View style={styles.input}>
                        <Text style={styles.inputLabel}>Password</Text>
                        <TextInput
                            secureTextEntry
                            style={styles.inputControl}
                            value={form.password}
                            onChangeText={(password) => setForm({ ...form, password })}
                        />
                    </View>
                    <TouchableOpacity
                        onPress={handleSignIn}
                        disabled={isLoading}
                        style={[styles.btn, isLoading && { opacity: 0.5 }]}
                    >
                        <Text style={styles.btnText}>{isLoading ? 'Signing in...' : 'Sign in'}</Text>
                    </TouchableOpacity>
                    <TouchableOpacity onPress={() => navigation.navigate('SignUp')}>
                        <Text style={styles.formFooter}>
                            Don't have an account?{' '}
                            <Text style={{ textDecorationLine: 'underline' }}>Sign up</Text>
                        </Text>
                    </TouchableOpacity>
                </View>
            </View>
        </SafeAreaView>
    );
}

