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
            await login(user, token);
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
            if (error.response) {
                console.log('Error response:', error.response.data, error.response.status);
                Alert.alert('Error', error.response.data.message || 'Login failed');
            } else if (error.request) {
                console.log('Error request:', error.request);
                Alert.alert('Error', 'No response from server');
            } else {
                console.log('Error message:', error.message);
                Alert.alert('Error', error.message);
            }
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

